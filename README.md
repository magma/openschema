# OpenSchemaSDK

### Getting OpenSchemaSDK library

#### Swift Package Manager

The Swift Package Manager is the preferred way to get OpenSchemaSDK Swift. Simply add the
package dependency to your `Package.swift` and depend on `"OpenSchemaSDK"` in the
necessary targets:

```swift
dependencies: [
  .package(url: "https://github.com/shoelacewireless/mma-ios.git", from: "x.x.x")
]
```

The syntax for target dependencies changed in Swift 5.2 and requires the package
of each dependency to be specified.

For Swift 5.2 (`swift-tools-version:5.2`):

```swift
.target(
  name: ...,
  dependencies: [.product(name: "OpenSchemaSDK", package: "OpenSchemaSDK")]
)
```
