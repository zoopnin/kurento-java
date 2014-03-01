package com.kurento.tool.rom.transport.jsonrpcconnector;

import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_CONSTRUCTOR_PARAMS;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.CREATE_TYPE;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OBJECT;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_NAME;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.INVOKE_OPERATION_PARAMS;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_DATA;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_OBJECT;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_SUBSCRIPTION;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.ONEVENT_TYPE;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.RELEASE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_METHOD;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_OBJECT;
import static com.kurento.tool.rom.transport.jsonrpcconnector.RomJsonRpcConstants.SUBSCRIBE_TYPE;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Function;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.media.Continuation;
import com.kurento.tool.rom.client.RomClient;
import com.kurento.tool.rom.client.RomEventHandler;
import com.kurento.tool.rom.server.RomException;
import com.kurento.tool.rom.transport.serialization.ParamsFlattener;

public class RomClientJsonRpcClient extends RomClient {

	private JsonRpcClient client;

	public RomClientJsonRpcClient(JsonRpcClient client) {
		this.client = client;
	}

	@Override
	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type) {
		return invoke(objectRef, operationName, operationParams, type, null);
	}

	@Override
	public String subscribe(String objectRef, String type) {
		return subscribe(objectRef, type, null);
	}

	@Override
	public String create(String remoteClassName, Props constructorParams)
			throws RomException {
		return create(remoteClassName, constructorParams, null);
	}

	@Override
	public void release(String objectRef) throws RomException {
		release(objectRef, null);
	}

	@Override
	public String create(String remoteClassName, Props constructorParams,
			Continuation<String> cont) throws RomException {

		JsonObject params = new JsonObject();
		params.addProperty(CREATE_TYPE, remoteClassName);
		if (constructorParams != null) {

			constructorParams = ParamsFlattener.getInstance().flattenParams(
					constructorParams);

			params.add(CREATE_CONSTRUCTOR_PARAMS,
					JsonUtils.toJsonObject(constructorParams));
		}

		return this.<String, String> sendRequest(CREATE_METHOD, String.class,
				params, null, cont);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E invoke(String objectRef, String operationName,
			Props operationParams, Class<E> clazz) throws RomException {
		return (E) invoke(objectRef, operationName, operationParams,
				(Type) clazz);
	}

	@Override
	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type, Continuation<?> cont)
			throws RomException {

		JsonObject params = new JsonObject();
		params.addProperty(INVOKE_OBJECT, objectRef);
		params.addProperty(INVOKE_OPERATION_NAME, operationName);

		if (operationParams != null) {

			operationParams = ParamsFlattener.getInstance().flattenParams(
					operationParams);

			params.add(INVOKE_OPERATION_PARAMS,
					JsonUtils.toJsonObject(operationParams));
		}

		return sendRequest(INVOKE_METHOD, type, params, null, cont);
	}

	@Override
	public void release(String objectRef, Continuation<Void> cont)
			throws RomException {

		JsonObject params = JsonUtils.toJsonObject(new Props("object",
				objectRef));

		sendRequest(RELEASE_METHOD, Void.class, params, null, cont);
	}

	@Override
	public String subscribe(String objectRef, String eventType,
			Continuation<String> cont) {

		JsonObject params = JsonUtils.toJsonObject(new Props(SUBSCRIBE_OBJECT,
				objectRef).add(SUBSCRIBE_TYPE, eventType));

		Function<JsonElement, String> processor = new Function<JsonElement, String>() {
			@Override
			public String apply(JsonElement subscription) {

				if (subscription instanceof JsonPrimitive) {
					return subscription.getAsString();
				} else {

					JsonObject subsObject = (JsonObject) subscription;
					Set<Entry<String, JsonElement>> entries = subsObject
							.entrySet();
					if (entries.size() != 1) {
						throw new RomException(
								"Error format in response to subscription operation."
										+ "The response should have one property and it has "
										+ entries.size()
										+ ". The response is: " + subscription);
					} else {
						return entries.iterator().next().getValue()
								.getAsString();
					}
				}
			}
		};

		return sendRequest(SUBSCRIBE_METHOD, JsonElement.class, params,
				processor, cont);
	}

	@Override
	public void addRomEventHandler(final RomEventHandler eventHandler) {

		this.client
				.setServerRequestHandler(new DefaultJsonRpcHandler<JsonObject>() {

					@Override
					public void handleRequest(Transaction transaction,
							Request<JsonObject> request) throws Exception {
						processEvent(eventHandler, request);
					}
				});
	}

	private void processEvent(RomEventHandler eventHandler,
			Request<JsonObject> request) {

		JsonObject params = request.getParams();

		String objectRef = params.get(ONEVENT_OBJECT).getAsString();
		String subscription = params.get(ONEVENT_SUBSCRIPTION).getAsString();
		String type = params.get(ONEVENT_TYPE).getAsString();
		JsonObject jsonData = (JsonObject) params.get(ONEVENT_DATA);
		Props data = JsonUtils.fromJson(jsonData, Props.class);

		eventHandler.processEvent(objectRef, subscription, type, data);
	}

	public void destroy() {
		try {
			client.close();
		} catch (IOException e) {
			throw new RuntimeException("Exception while closing JsonRpcClient",
					e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P, R> R sendRequest(String method, final Type type,
			JsonObject params, final Function<P, R> processor,
			final Continuation<R> cont) {
		try {

			if (cont == null) {

				return processReqResult(type, processor,
						client.sendRequest(method, params, JsonElement.class));

			} else {

				client.sendRequest(
						method,
						params,
						new com.kurento.kmf.jsonrpcconnector.client.Continuation<JsonElement>() {

							@SuppressWarnings({ "rawtypes" })
							@Override
							public void onSuccess(JsonElement reqResult) {

								R methodResult = processReqResult(type,
										processor, reqResult);
								((Continuation) cont).onSuccess(methodResult);
							}

							@Override
							public void onError(Throwable cause) {
								cont.onError(cause);
							}
						});

				return null;
			}

		} catch (IOException e) {
			throw new RomException("Exception while sending request", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P, R> R processReqResult(final Type type,
			Function<P, R> processor, JsonElement reqResult) {
		P methodResult = convertFromResult(reqResult, type);

		if (processor == null) {
			return (R) methodResult;
		} else {
			return processor.apply(methodResult);
		}
	}

	private <E> E convertFromResult(JsonElement result, Type type) {

		if (type == Void.class || type == void.class) {
			return null;
		}

		JsonElement extractResult = extractValueFromResponse(result, type);

		return JsonUtils.fromJson(extractResult, type);
	}

	private JsonElement extractValueFromResponse(JsonElement result, Type type) {

		if (result == null) {
			return null;
		}

		if (isList(type) || isPrimitiveClass(type) || isEnum(type)) {

			if (result instanceof JsonPrimitive) {
				return result;

			} else if (result instanceof JsonArray) {
				throw new RomException("Json array '" + result
						+ " cannot be converted to " + getTypeName(type));

			} else if (result instanceof JsonObject) {

				JsonObject respObject = (JsonObject) result;

				if (!respObject.has("value")) {
					throw new RomException("Json object " + result
							+ " cannot be converted to " + getTypeName(type)
							+ " without a 'value' property");
				}

				return respObject.get("value");

			} else {
				throw new RomException("Unrecognized json element: " + result);
			}

		} else {
			return result;
		}
	}

	private boolean isEnum(Type type) {

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			return clazz.isEnum();
		}

		return false;
	}

	private boolean isPrimitiveClass(Type type) {
		return type == String.class || type == Integer.class
				|| type == Float.class || type == Boolean.class
				|| type == int.class || type == float.class
				|| type == boolean.class;
	}

	private boolean isList(Type type) {

		if (type == List.class) {
			return true;
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			if (pType.getRawType() instanceof Class) {
				return ((Class<?>) pType.getRawType())
						.isAssignableFrom(List.class);
			}
		}

		return false;
	}

	private String getTypeName(Type type) {

		if (type instanceof Class) {

			Class<?> clazz = (Class<?>) type;
			return clazz.getSimpleName();

		} else if (type instanceof ParameterizedType) {

			StringBuilder sb = new StringBuilder();

			ParameterizedType pType = (ParameterizedType) type;
			Class<?> rawClass = (Class<?>) pType.getRawType();

			sb.append(rawClass.getSimpleName());

			Type[] arguments = pType.getActualTypeArguments();
			if (arguments.length > 0) {
				sb.append("<");
				for (Type aType : arguments) {
					sb.append(getTypeName(aType));
					sb.append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(">");
			}

			return sb.toString();
		}

		return type.toString();
	}

}