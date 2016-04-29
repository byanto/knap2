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
package org.knime.base.node.audio2.data.cell;

import java.io.IOException;
import java.util.UUID;

import org.knime.base.node.audio2.data.Audio;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.NodeLogger;

/**
 * Cell factory to create audio cell.
 * @author Budi Yanto, KNIME.com
 */
public class AudioCellFactory implements DataCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AudioCellFactory.class);

    /** The maximum file store size used to store audio, default: 10 MB */
    private static final long MAX_FILE_STORE_SIZE = 10 * 1024l * 1024l;

    /** The estimated current file store size in bytes */
    private long m_currentFileStoreSize = 0;

    private FileStore m_fileStore;
    private FileStoreFactory m_fileStoreFactory;

    /**
     *
     * @param fileStoreFactory
     */
    public AudioCellFactory(final FileStoreFactory fileStoreFactory){
        m_fileStoreFactory = fileStoreFactory;
    }

    /**
     * Creates a new <code>AudioCell</code>.
     *
     * @param audio the <code>Audio</code> used to create the <code>AudioCell</code>.
     * @return a new <code>AudioCell</code> containing the given <code>Audio</code>.
     */
    public AudioCell createCell(final Audio audio){
        final long audioSize = audio.getSize();
        updateFileStore(audioSize);
        AudioCell cell = new AudioCell(m_fileStore, audio);
        m_currentFileStoreSize += audioSize;
        return cell;
    }

    /**
     * Update the file store. If the size of the current file store exceeds
     * the maximum size, then create a new file store.
     * Otherwise use the existing one.
     * @param size the approx. size of the next audio to write (in Bytes).
     */
    private void updateFileStore(final long size) {
        if((m_fileStore == null) ||
                ((m_currentFileStoreSize + size) >= MAX_FILE_STORE_SIZE)){
            final String fileStoreUUID = UUID.randomUUID().toString();
            LOGGER.debug("Creating a new file store: " + fileStoreUUID
                + ". The size of the last file store is approx. "
                + (m_currentFileStoreSize / (1024.0 * 1024.0)) + " MB.");
            try{
                m_fileStore = m_fileStoreFactory.createFileStore(fileStoreUUID);
            } catch(IOException ex){
                LOGGER.error("Could not create file store.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return AudioCell.TYPE;
    }
}
