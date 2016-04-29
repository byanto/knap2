/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created on Oct 9, 2015 by dietzc
 */
package org.knime.base.node.audio2.data.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.base.node.audio2.util.KNAPConstants;

/**
 * Util class for handling streams used by cell implementations.
 *
 * @author Christian Dietz, University of Konstanz
 */
public class StreamUtil {

    /**
     * Helper to create the respective input stream (e.g. if zip file or not)
     * @param file the file to read from
     * @param offset
     * @throws IOException
     * @return the input stream
     */
    public static BufferedDataInputStream createInputStream(final File file, final long offset) throws IOException {
        BufferedDataInputStream stream = null;
        try {
            if (file.getName().endsWith(KNAPConstants.ZIP_SUFFIX)) {
                final FileInputStream fileInput = new FileInputStream(file);
                fileInput.skip(offset);
                final ZipInputStream zip = new ZipInputStream(fileInput);
                zip.getNextEntry();
                stream = new BufferedDataInputStream(zip);
            } else {
                stream = new BufferedDataInputStream(new FileInputStream(file));
                stream.skip(offset);
            }
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
            throw e;
        }
        return stream;
    }

    /**
     * Helper to create the respective output stream (e.g. if zip file or not)
     * @param file the file to write to
     * @throws FileNotFoundException
     * @throws IOException
     * @return the output stream
     */
    public static BufferedDataOutputStream createOutStream(final File file) throws FileNotFoundException, IOException {
        BufferedDataOutputStream stream;
        if (file.getName().endsWith(KNAPConstants.ZIP_SUFFIX)) {
            final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file, true));
            zip.putNextEntry(new ZipEntry("audio"));
            stream = new BufferedDataOutputStream(zip);
        } else {
            stream = new BufferedDataOutputStream(new FileOutputStream(file, true));
        }
        return stream;
    }
}
