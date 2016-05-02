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
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.knime.core.node.NodeLogger;

import jAudioFeatureExtractor.jAudioTools.AudioMethods;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.DoubleType;

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

    public static Img<DoubleType> getSamples(final AudioInputStream audioInputStream)
            throws UnsupportedAudioFileException, IOException{

        Img<DoubleType> samples = null;
        try{
            samples = extractSampleValues(audioInputStream);
        } catch(Exception ex){
            LOGGER.error(ex.getMessage());
        }

//        audioInputStream.close();




//        if(channelSamples == null){
//            return null;
//        }
        return samples;
//        return DSPMethods.getSamplesMixedDownIntoOneChannel(channelSamples);

    }

    /**
     * This method is only compatible with audio with bit depth of 8 or 16 bits
     * that is encoded using PCM-Signed Big-Endian
     * @return
     */
    private static Img<DoubleType> extractSampleValues(
            final AudioInputStream audioInputStream) throws Exception{

        // Converts the contents of audio_input_stream into an array of bytes
        byte[] audioBytes = AudioMethods.getBytesFromAudioInputStream(audioInputStream);
        int nrOfBytes = audioBytes.length;

        // Note the AudioFormat
        AudioFormat inFormat = audioInputStream.getFormat();

        // Extract information from this_audio_format
        int nrOfChannels = inFormat.getChannels();
        int bitDepth = inFormat.getSampleSizeInBits();

        // Throw exception if incompatible this_audio_format provided
        if ( (bitDepth != 16 && bitDepth != 8 )||
             !inFormat.isBigEndian() ||
             inFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED ) {
            throw new Exception( "Only 8 or 16 bit signed PCM samples with a big-endian\n" +
                                 "byte order can be analyzed currently." );
        }

        // Find the number of samples in the audio_bytes
        int bytesPerSample = bitDepth / 8;
        int nrOfSamples = nrOfBytes / bytesPerSample / nrOfChannels;

        // Throw exception if incorrect number of bytes given
        if ( ((nrOfSamples == 2 || bytesPerSample == 2) && (nrOfBytes % 2 != 0)) ||
             ((nrOfSamples == 2 && bytesPerSample == 2) && (nrOfBytes % 4 != 0)) ) {
            throw new Exception("Uneven number of bytes for given bit depth and number of channels.");
        }

        // Find the maximum possible value that a sample may have with the given
        // bit depth
        double maxSampleValue = AudioMethods.findMaximumSampleValue(bitDepth) + 2.0;

        ImgFactory<DoubleType> factory = new ArrayImgFactory<DoubleType>();
        final long[] dims = new long[]{nrOfSamples, nrOfChannels};

        Img<DoubleType> imgSamples = factory.create(dims, new DoubleType());
        RandomAccess<DoubleType> randomAccess = imgSamples.randomAccess();

        // Convert the bytes to double samples
        ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes);
        if (bitDepth == 8)
        {
            for(int samp = 0; samp < nrOfSamples; samp++){
                randomAccess.setPosition(samp, KNAPConstants.SAMPLES_DIMENSION);
                for(int channel = 0; channel < nrOfChannels; channel++){
                    randomAccess.setPosition(channel,
                        KNAPConstants.CHANNEL_DIMENSION);
                    final DoubleType val = randomAccess.get();
                    val.set(byteBuffer.get() / maxSampleValue);
                }
            }
        }
        else if (bitDepth == 16)
        {
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
            for(int samp = 0; samp < nrOfSamples; samp++){
                randomAccess.setPosition(samp, KNAPConstants.SAMPLES_DIMENSION);
                for(int channel = 0; channel < nrOfChannels; channel++){
                    randomAccess.setPosition(channel,
                        KNAPConstants.CHANNEL_DIMENSION);
                    final DoubleType val = randomAccess.get();
                    val.set(shortBuffer.get() / maxSampleValue);
                }
            }
        }

        // Return the samples
        return imgSamples;

    }

    /**
     * Convert multichannels audio to single channel (mono)
     * @param samples
     * @return a single channel audio
     */
    public static Img<DoubleType> convertSamplesToMono(final Img<DoubleType> samples){
        if(samples.numDimensions() == 1){
            return samples;
        }

        RandomAccess<DoubleType> randomAccess = samples.randomAccess();
        long nrOfSamples = samples.dimension(KNAPConstants.SAMPLES_DIMENSION);
        long nrOfChannels = samples.dimension(KNAPConstants.CHANNEL_DIMENSION);

        final ImgFactory<DoubleType> factory = samples.factory();
        final Img<DoubleType> result = factory.create(
            new long[]{nrOfSamples}, samples.firstElement());
        Cursor<DoubleType> cursor = result.cursor();
        cursor.reset();
        for(long samp = 0; samp < nrOfSamples; samp++){
            randomAccess.setPosition(samp, KNAPConstants.SAMPLES_DIMENSION);
            double total = 0.0;
            for(long channel = 0; channel < nrOfChannels; channel++){
                randomAccess.setPosition(channel, KNAPConstants.CHANNEL_DIMENSION);
                total += randomAccess.get().get();
            }

            cursor.next().set(total / nrOfChannels);
        }

        return result;
    }

//    public static double[] getSamplesMixedDownIntoOneChannel(
//            final AudioInputStream audioInputStream) throws UnsupportedAudioFileException, IOException {
//        final double[][] samples = getSamples(audioInputStream);
//        return DSPMethods.getSamplesMixedDownIntoOneChannel(samples);
//    }

}
