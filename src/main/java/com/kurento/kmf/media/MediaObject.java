package com.kurento.kmf.media;

import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;

public interface MediaObject {

	/**
	 * Explicitly release a media object form memory. All of its children will
	 * also be released
	 * 
	 */
	public void release();

	/**
	 * This methods subscribes to events generated by this media object.
	 * 
	 * @param handlerAddress
	 * @param handlerPort
	 */
	public <E extends MediaEvent> ListenerRegistration addListener(
			String eventType, MediaEventListener<E> listener);

	public <E extends MediaEvent> void removeListener(
			ListenerRegistration listenerRegistartion);

	/**
	 * Returns the parent of this media object. The type of the parent depends
	 * on the type of the element that this method is called upon. <li>
	 * MediaPad->MediaElement</li> <li>MediaMixer->MediaPipeline</li> <li>
	 * MediaElement->MediaPipeline</li> <li>MediaPipeline->null</li>
	 * 
	 * @return The parent
	 */
	public MediaObject getParent();

	/**
	 * Returns the pipeline to which this MediaObject belong, or the pipeline
	 * itself if invoked over a {@link MediaPipeline}
	 * 
	 * @return The media pipeline for the object, or <code>this</code> in case
	 *         of a media pipeline
	 */
	public MediaPipeline getMediaPipeline();

	/**
	 * Explicitly release a media object form memory. All of its children will
	 * also be released
	 * 
	 */
	public void release(final Continuation<Void> cont);

	/**
	 * This methods subscribes to events generated by this media object.
	 * 
	 * @param handlerAddress
	 * @param handlerPort
	 */
	public <E extends MediaEvent> void addListener(
			final MediaEventListener<E> listener, final String eventType,
			final Continuation<ListenerRegistration> cont);

	public <E extends MediaEvent> void removeListener(
			ListenerRegistration listenerRegistration, Continuation<Void> cont);

	/**
	 * Returns the parent of this media object. The type of the parent depends
	 * on the type of the element that this method is called upon. <li>
	 * MediaPad->MediaElement</li> <li>MediaMixer->MediaPipeline</li> <li>
	 * MediaElement->MediaPipeline</li> <li>MediaPipeline->null</li>
	 * 
	 * @return The parent
	 */
	public <F extends MediaObject> void getParent(final Continuation<F> cont);

	/**
	 * Returns the pipeline to which this MediaObject belongs, or the pipeline
	 * itself if invoked over a {@link MediaPipeline}
	 * 
	 * @return The media pipeline for the object, or <code>this</code> in case
	 *         of a media pipeline
	 */
	public void getMediaPipeline(final Continuation<MediaPipeline> cont);

}