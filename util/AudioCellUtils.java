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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.base.node.audio2.data.Audio;
import org.knime.base.node.audio2.data.AudioBuilder;
import org.knime.base.node.audio2.data.AudioMetadata;
import org.knime.base.node.audio2.data.io.BufferedDataInputStream;
import org.knime.base.node.audio2.data.io.BufferedDataOutputStream;
import org.knime.base.node.audio2.data.io.StreamUtil;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.DoubleType;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class AudioCellUtils {

    /**
    * Convert the given audio to byte array
    * @param audio audio to convert
    * @return the byte array of the given audio
    */
   public static byte[] convertToByteArray(final Audio audio) throws IOException{
       final ByteArrayOutputStream out = new ByteArrayOutputStream();
       final BufferedDataOutputStream buf = new BufferedDataOutputStream(out);
       buf.writeUTF(audio.getUuid().toString());

       out.flush();
       return out.toByteArray();
   }

   /**
    * Convert the given byte array to audio
    * @param bytes the byte array to convert
    * @return the audio representation of the byte array
    */
   public static Audio convertToAudio(final byte[] bytes){
       return null;
   }

   public static void serialize(final Audio audio, final File file)
           throws FileNotFoundException, IOException{
       final BufferedDataOutputStream outStream = StreamUtil.createOutStream(file);

       final Img<DoubleType> samples = audio.getSamples();

       /* Write audio metadata to the output stream */
       final AudioMetadata metadata = audio.getMetadata();
       outStream.writeUTF(metadata.getFilePath());


       /* Write audio samples to the output stream */
       // write dimensions
       outStream.writeInt(samples.numDimensions());
       for (int i = 0; i < samples.numDimensions(); i++) {
           outStream.writeLong(samples.dimension(i));
       }

       final Cursor<DoubleType> cur = samples.cursor();
       while(cur.hasNext()){
           cur.fwd();
           outStream.writeDouble(cur.get().get());
       }

       /* Flush and close the output stream */
       outStream.flush();
       outStream.close();

   }

   public static Audio deserialize(final File file) throws IOException{
       final BufferedDataInputStream inStream = StreamUtil.createInputStream(file, 0);
       /* Read audio metadata from the input stream */


       /* Read audio samples from the input stream */
       ImgFactory<DoubleType> factory = new ArrayImgFactory<DoubleType>();

       final long[] dims = new long[inStream.readInt()];
       inStream.read(dims);

       final Img<DoubleType> samples = factory.create(dims, new DoubleType());
       final Cursor<DoubleType> cur = samples.cursor();

       final int totalSize = (int)samples.size();
       final int buffSize = 8192;

       cur.reset();

       final double[] doubleBuf = new double[Math.min(buffSize, totalSize)];

       int currIdx = 0;
       while (currIdx < totalSize) {
           inStream.read(doubleBuf, 0, Math.min(doubleBuf.length, totalSize - currIdx));

           int idx = 0;
           while (cur.hasNext() && (idx < buffSize)) {
               cur.fwd();
               cur.get().set(doubleBuf[idx++]);
           }
           currIdx += idx;
       }



       /* Close the input stream */
       inStream.close();

       return AudioBuilder.createAudio(null, samples);
   }



}
