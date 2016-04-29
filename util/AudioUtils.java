/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 28, 2016 (budiyanto): created
 */
package org.knime.base.node.audio2.util;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.knime.core.node.NodeLogger;

import jAudioFeatureExtractor.jAudioTools.AudioMethods;
import jAudioFeatureExtractor.jAudioTools.DSPMethods;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class AudioUtils {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AudioUtils.class);

    /**
     * Normalizes bytes from bits.
     *
     * Some formats allow for bit depths in non-multiples of 8.
     * they will, however, typically pad so the samples are stored
     * that way. AIFF is one of these formats.
     *
     * so the expression:
     *
     *  bitsPerSample + 7 >> 3
     *
     * computes a division of 8 rounding up (for positive numbers).
     *
     * this is basically equivalent to:
     *
     * (int)Math.ceil(bitsPerSample / 8.0)
     *
     * @param bitsPerSample bits to normalize
     * @return the normalized bytes
     */
    public static int normalizeBytesFromBits(final int bitsPerSample) {
        return bitsPerSample + 7 >> 3;
    }

    public static int normalizeBitDepthFromBits(final int bitsPerSample){
        return normalizeBytesFromBits(bitsPerSample) * 8;
    }

    public static double[][] getSamples(final AudioInputStream audioInputStream)
            throws UnsupportedAudioFileException, IOException{

        AudioFormat format = audioInputStream.getFormat();
        final int bitDepth = normalizeBitDepthFromBits(format.getSampleSizeInBits());

        // If the audio is not PCM signed big endian, then convert it to PCM
        // signed. This is particularly necessary when dealing with MP3s
        AudioInputStream newStream = audioInputStream;
        if(format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED ||
                !format.isBigEndian()){
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(), bitDepth, format.getChannels(),
                format.getChannels() * (bitDepth / 8), format.getFrameRate(), true);
            newStream = AudioSystem.getAudioInputStream(format, audioInputStream);
        }

        double[][] channelSamples = null;
        try{
            channelSamples = AudioMethods.extractSampleValues(newStream);
        } catch(Exception ex){
            LOGGER.error(ex.getMessage());
        }

//        audioInputStream.close();


        if(newStream != null){
            newStream.close();
        }

//        if(channelSamples == null){
//            return null;
//        }
        return channelSamples;
//        return DSPMethods.getSamplesMixedDownIntoOneChannel(channelSamples);

    }

    public static double[] getSamplesMixedDownIntoOneChannel(
            final AudioInputStream audioInputStream) throws UnsupportedAudioFileException, IOException {
        final double[][] samples = getSamples(audioInputStream);
        return DSPMethods.getSamplesMixedDownIntoOneChannel(samples);
    }

}
