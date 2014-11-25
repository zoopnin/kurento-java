/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.stability.webrtc;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.StabilityTest;
import org.kurento.test.client.Browser;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.color.LatencyController;
import org.kurento.test.color.VideoTag;

/**
 * <strong>Description</strong>: Stability test for switching 2 WebRTC (looback
 * to back-2-back) a configurable number of times (each switch holds 1 second).<br/>
 * <strong>Pipeline(s)</strong>:
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (loopback)</li>
 * <li>... to:</li>
 * <li>WebRtcEndpoint -> WebRtcEndpoint (back to back)</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>No latency problems detected during test time</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */

public class WebRtcStabilitySwitchTest extends StabilityTest {

	private static final int DEFAULT_NUM_SWITCH = 60; // 1 switch = 1 minute

	@Test
	public void testWebRtcSwitchChrome() throws InterruptedException {
		final int numSwitch = Integer.parseInt(System.getProperty(
				"test.webrtcstability.switch",
				String.valueOf(DEFAULT_NUM_SWITCH)));
		doTest(Browser.CHROME, getPathTestFiles() + "/video/15sec/rgb.y4m",
				numSwitch);
	}

	public void doTest(Browser browserType, String videoPath, int numSwitch)
			throws InterruptedException {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint1 = new WebRtcEndpoint.Builder(mp).build();
		WebRtcEndpoint webRtcEndpoint2 = new WebRtcEndpoint.Builder(mp).build();
		webRtcEndpoint1.connect(webRtcEndpoint1);
		webRtcEndpoint2.connect(webRtcEndpoint2);

		BrowserClient.Builder builder = new BrowserClient.Builder().browser(
				browserType).client(Client.WEBRTC);
		if (videoPath != null) {
			builder = builder.video(videoPath);
		}

		try (BrowserClient browser1 = builder.build();
				BrowserClient browser2 = builder.build()) {
			browser1.initWebRtc(webRtcEndpoint1, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);
			browser2.initWebRtc(webRtcEndpoint2, WebRtcChannel.VIDEO_ONLY,
					WebRtcMode.SEND_RCV);

			for (int i = 0; i < numSwitch; i++) {
				if (i % 2 == 0) {
					log.debug("Switch #" + i + ": loopback");
					webRtcEndpoint1.connect(webRtcEndpoint1);
					webRtcEndpoint2.connect(webRtcEndpoint2);

					// Latency control (loopback)
					log.debug("[{}.1] Latency control of browser1 to browser1",
							i);
					LatencyController cs1 = new LatencyController();
					browser1.addChangeColorEventListener(VideoTag.LOCAL, cs1,
							"1to1_loc" + i);
					browser1.addChangeColorEventListener(VideoTag.REMOTE, cs1,
							"1to1_rem" + i);
					cs1.checkLatency(30, TimeUnit.SECONDS);

					log.debug("[{}.2] Latency control of browser2 to browser2",
							i);
					LatencyController cs2 = new LatencyController();
					browser1.addChangeColorEventListener(VideoTag.LOCAL, cs2,
							"2to2_loc" + i);
					browser1.addChangeColorEventListener(VideoTag.REMOTE, cs2,
							"2to2_loc" + i);
					cs2.checkLatency(30, TimeUnit.SECONDS);

				} else {
					log.debug("Switch #" + i + ": B2B");
					webRtcEndpoint1.connect(webRtcEndpoint2);
					webRtcEndpoint2.connect(webRtcEndpoint1);

					// Latency control (B2B)
					log.debug("[{}.3] Latency control of browser1 to browser2",
							i);
					LatencyController cs1 = new LatencyController();
					browser1.addChangeColorEventListener(VideoTag.LOCAL, cs1,
							"1to2_loc" + i);
					browser2.addChangeColorEventListener(VideoTag.REMOTE, cs1,
							"1to2_rem" + i);
					cs1.checkLatency(30, TimeUnit.SECONDS);

					log.debug("[{}.4] Latency control of browser2 to browser1",
							i);
					LatencyController cs2 = new LatencyController();
					browser2.addChangeColorEventListener(VideoTag.LOCAL, cs2,
							"2to1_loc" + i);
					browser1.addChangeColorEventListener(VideoTag.REMOTE, cs2,
							"2to1_rem" + i);
					cs2.checkLatency(30, TimeUnit.SECONDS);
				}
			}
		}

		// Release Media Pipeline
		mp.release();
	}
}
