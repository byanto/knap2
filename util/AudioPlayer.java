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
 *   May 2, 2016 (budiyanto): created
 */
package org.knime.base.node.audio2.util;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.knime.base.node.audio2.data.Audio;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class AudioPlayer implements Runnable{

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AudioPlayer.class);

    private List<AudioEventListener> m_listeners = new ArrayList<AudioEventListener>();

    private boolean m_started = false;

    private Audio m_audio = null;

    private SourceDataLine m_line = null;

    private STATUS m_status = STATUS.PLAY;

    /**
     * Enumerator for the current status of the audio player
     */
    public enum STATUS {
        /** The audio player is playing */
        PLAY,

        /** The audio player is paused */
        PAUSE,

        /** The audio player is stopped */
        STOP
    }

    public AudioPlayer(final Audio audio){
        m_audio = audio;
    }

    public AudioPlayer(final Audio audio, final AudioEventListener... listeners){
        m_audio = audio;
        for(AudioEventListener listener : listeners){
            m_listeners.add(listener);
        }
    }

    /**
     * Adds a listener to the list
     * @param listener the listener to add
     */
    public void addAudioEventListener(final AudioEventListener listener){
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from the list
     * @param listener the listener to remove
     */
    public void removeAudioEventListener(final AudioEventListener listener){
        m_listeners.remove(listener);
    }

    private void fireBeforePlay(){
        for(final AudioEventListener listener : m_listeners){
            listener.beforeEvent();
        }
    }

    private void fireAfterPlay(){
        for(final AudioEventListener listener : m_listeners){
            listener.afterEvent();
        }
    }

    private void setStatus(final STATUS status){
        m_status = status;
    }

    private void openSourceDataLine() throws Exception{

        final AudioFormat format = m_audio.getMetadata().getAudioFormat();
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        m_line = (SourceDataLine)AudioSystem.getLine(info);
        if (m_line == null) {
            throw new Exception("Cannot open source data line");
        }

        m_line.open(m_line.getFormat());
        m_line.start();
        LOGGER.debug("Opened Source Data Line: " + m_line.getFormat()
            + " with buffer size: " + m_line.getBufferSize());

    }

    private void closeSourceDataLine(){
        if(m_line != null){
            m_line.drain();
            m_line.close();
            m_line = null;
        }
    }

    private void playSound(){

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        setStatus(STATUS.PLAY);
        if(!m_started){
            m_started = true;
            try{
                openSourceDataLine();
                boolean ended = false;
                while(!ended && m_status != STATUS.STOP){
                    if(m_status == STATUS.PLAY){
                        fireBeforePlay();
                        playSound();
                        fireAfterPlay();
                    }else{ // Player is in PAUSE
                        try{
                            Thread.sleep(500);
                        } catch(final InterruptedException ex){

                        }
                    }
                }
                setStatus(STATUS.STOP);
                reset();
            } catch (Exception ex){
                LOGGER.error(ex.getMessage());
            } finally{
                closeSourceDataLine();
            }
        }else{
            setStatus(STATUS.PLAY);
        }
    }

    private void reset(){
        m_started = false;
    }

}
