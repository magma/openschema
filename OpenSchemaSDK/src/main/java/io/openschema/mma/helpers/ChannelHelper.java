/*
 * Copyright (c) 2020, The Magma Authors
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openschema.mma.helpers;

import javax.net.ssl.SSLSocketFactory;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

/**
 * Helper class to handle creation of gRPC channels.
 */
public class ChannelHelper {

    public static ManagedChannel getSecureManagedChannel(
            String host,
            int port,
            SSLSocketFactory factory) {
        return OkHttpChannelBuilder
                .forAddress(host, port)
                .useTransportSecurity()
                .sslSocketFactory(factory)
                .build();
    }

    public static ManagedChannel getSecureManagedChannelwithAuthorityHeader(
            String host,
            int port,
            SSLSocketFactory factory,
            String authority) {
        return OkHttpChannelBuilder
                .forAddress(host, port)
                .useTransportSecurity()
                .sslSocketFactory(factory)
                .overrideAuthority(authority)
                .build();
    }
}
