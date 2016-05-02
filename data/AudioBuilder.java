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
 *   Apr 22, 2016 (budiyanto): created
 */
package org.knime.base.node.audio2.data;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.knime.base.node.audio2.util.AudioUtils;
import org.knime.base.node.audio2.util.Validator;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class AudioBuilder {

    private File m_file;

    public static Audio createAudio(final AudioMetadata metadata, final Img<DoubleType> samples){
        return new Audio(metadata, samples);
    }

    public static Audio createAudio(final String filePath)
            throws UnsupportedAudioFileException, IOException{

        final AudioInputStream audioStream = AudioSystem.getAudioInputStream(
            new File(filePath));

        AudioFormat format = audioStream.getFormat();
        final int bitDepth = AudioUtils.normalizeBitDepthFromBits(
            format.getSampleSizeInBits());

        // If the audio is not PCM signed big endian, then convert it to PCM
        // signed. This is particularly necessary when dealing with MP3s
        AudioInputStream newStream = audioStream;
        if(format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED ||
                !format.isBigEndian()){
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(), bitDepth, format.getChannels(),
                format.getChannels() * (bitDepth / 8), format.getFrameRate(), true);
            newStream = AudioSystem.getAudioInputStream(format, audioStream);
        }

        final AudioMetadata metadata = new AudioMetadata(filePath,
            newStream.getFormat());

        final Img<DoubleType> samples = AudioUtils.getSamples(newStream);

        if(newStream != audioStream){
            newStream.close();
        }
        audioStream.close();

        return new Audio(metadata, samples);
    }

    public static Audio[] createAudioArray(final String ... filePaths){
        return null;
    }

    public void setFile(final String filePath){
        if(Validator.validateFile(filePath)){
            m_file = new File(filePath);
        }
    }

    public Audio createAudio(){
        return new Audio();
    }
}
