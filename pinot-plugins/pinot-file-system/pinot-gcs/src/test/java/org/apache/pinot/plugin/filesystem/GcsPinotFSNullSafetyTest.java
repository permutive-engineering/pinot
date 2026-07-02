/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.plugin.filesystem;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;


/**
 * Unit tests verifying null-safety of the ranged-read SPI in GcsPinotFS:
 * - openForRead() throws FileNotFoundException (not NPE) when the blob does not exist
 */
public class GcsPinotFSNullSafetyTest {

  private Storage _mockStorage;
  private GcsPinotFS _gcsPinotFS;

  @BeforeMethod
  public void setUp()
      throws Exception {
    _mockStorage = mock(Storage.class);
    _gcsPinotFS = new GcsPinotFS();
    Field storageField = GcsPinotFS.class.getDeclaredField("_storage");
    storageField.setAccessible(true);
    storageField.set(_gcsPinotFS, _mockStorage);
  }

  @Test
  public void testOpenForReadThrowsFileNotFoundExceptionWhenBlobDoesNotExist()
      throws IOException {
    URI uri = URI.create("gs://test-bucket/missing-file");
    when(_mockStorage.get(any(BlobId.class))).thenReturn(null);

    try {
      _gcsPinotFS.openForRead(uri, 0, 128);
      fail("Expected FileNotFoundException");
    } catch (FileNotFoundException ex) {
      assertEquals(ex.getMessage(), "File '" + uri + "' does not exist");
    }
  }

  @Test
  public void testOpenForReadDoesNotThrowNullPointerException()
      throws IOException {
    URI uri = URI.create("gs://test-bucket/missing-file");
    when(_mockStorage.get(any(BlobId.class))).thenReturn(null);

    // A ranged read against a missing object must throw FileNotFoundException, not NullPointerException
    assertThrows(FileNotFoundException.class, () -> _gcsPinotFS.openForRead(uri, 0, 128));
  }
}
