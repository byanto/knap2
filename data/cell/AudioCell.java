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
 *   Apr 21, 2016 (budiyanto): created
 */
package org.knime.base.node.audio2.data.cell;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.base.node.audio2.data.Audio;
import org.knime.base.node.audio2.util.AudioCellUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.util.LRUCache;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class AudioCell extends FileStoreCell implements AudioValue, StringValue {

    /**
     * Serializer for {@link AudioCell}s.
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer implements DataCellSerializer<AudioCell>{

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final AudioCell cell, final DataCellDataOutput output) throws IOException {
            cell.serializeCell(output);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AudioCell deserialize(final DataCellDataInput input) throws IOException {
            AudioCell cell = new AudioCell();
            cell.deserializeCell(input);
            return cell;
        }

    }

    /**
     * Convenience access member for
     * <code>DataType.getType(AudioCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(AudioCell.class);

    /**
     * Automatically generated Serial Version UUID
     */
    private static final long serialVersionUID = -3481283535341858517L;

    /** Audio to store */
    private Audio m_audio;

    /** Default cache size */
    private static final int DEF_CACHE_SIZE = 1000;

    /** LRUCache to cache the audio data */
    private static final LRUCache<UUID, Audio> AUDIO_CACHE = new LRUCache<UUID, Audio>(DEF_CACHE_SIZE);

    /** Flag to specify whether audio data of cell was serialized or not, in order to avoid multiple writes */
    private AtomicBoolean m_serialized = new AtomicBoolean(false);

    /** UUID as unique identifier of audio */
    private UUID m_audioUUID;
//
//    /** Offset used to mark the position of the audio data in file store */
//    private long m_offset;
//
//    /** Length of the byte array used to store the serialized audio */
//    private int m_length;

    /**
     * Empty constructor used for deserializing audio
     */
    AudioCell(){
        super();
    }

    /**
     * Creates a new instance of <code>AudioCell</code> with the given audio
     * and the file store to store the audio at.
     * @param fileStore File store to store the audio at
     * @param audio Audio to encapsulate and store in file store
     */
    public AudioCell(final FileStore fileStore, final Audio audio){
        super(fileStore);
        m_audio = audio;
        m_audioUUID = audio.getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        StringBuilder builder = new StringBuilder();
        builder.append("Audio[\npath=");
        builder.append(m_audio.getMetadata().getFilePath());
        builder.append("\n]");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Audio getAudio() {
        return m_audio;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // TODO: FIXME
        return getFileStore().getFile().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        if(dc == null){
            return false;
        }
        return m_audio.equals(((AudioValue)dc).getAudio());
    }

    /**
     *
     * @param output
     * @throws IOException
     */
    public void serializeCell(final DataCellDataOutput output) throws IOException {
//        flushToFileStore();
        output.writeUTF(m_audioUUID.toString());
    }

    /**
     *
     * @param input
     * @throws IOException
     */
    public void deserializeCell(final DataCellDataInput input) throws IOException {
        m_serialized = new AtomicBoolean(true);
        m_audioUUID = UUID.fromString(input.readUTF());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void flushToFileStore() throws IOException {
        if(m_serialized.compareAndSet(false, true)){
            AudioCellUtils.serialize(m_audio, getFileStore().getFile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void postConstruct() throws IOException {
        if(m_audio == null && m_audioUUID != null) {
            synchronized (AUDIO_CACHE) {
                m_audio = AUDIO_CACHE.get(m_audioUUID);
            }
            // Only deserialize if audio is not in cache
            if(m_audio == null){
                m_audio = AudioCellUtils.deserialize(getFileStore().getFile());
                synchronized (AUDIO_CACHE) {
                    final Audio audio = AUDIO_CACHE.get(m_audioUUID);
                    if(audio == null){
                        AUDIO_CACHE.put(m_audioUUID, m_audio);
                    }else{
                        m_audio = audio; // race condition, another thread "won"
                    }
                }
            }
        }
    }

}
