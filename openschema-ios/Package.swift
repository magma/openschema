// swift-tools-version:5.3
// The swift-tools-version declares the minimum version of Swift required to build this package.

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

import PackageDescription

let package = Package(
    name: "OpenSchemaSDK",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "OpenSchemaSDK",
            targets: ["OpenSchemaSDK"]),
    ],
    dependencies: [
        // Dependencies declare other packages that this package depends on.
        // .package(url: /* package url */, from: "1.0.0"),
        .package(url: "https://github.com/apple/swift-log.git", from: "1.0.0"),
        .package(name: "Reachability", url: "https://github.com/ashleymills/Reachability.swift.git", from: "5.1.0"),
        .package(name: "Runtime", url: "https://github.com/wickwirew/Runtime.git", from: "2.2.2")
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        .target(
            name: "OpenSchemaSDK",
            dependencies: [
                .product(name: "Logging", package: "swift-log"),
                .product(name: "Reachability", package: "Reachability"),
                .product(name: "Runtime", package: "Runtime")
            ]
        ),
        .testTarget(
            name: "OpenSchemaSDKTests",
            dependencies: ["OpenSchemaSDK"]),
    ]
)
