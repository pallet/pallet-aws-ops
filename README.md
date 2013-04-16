# pallet-aws-ops

Provide basic operations on AWS, on top of pallet amazonica.

Provides a forklift processor for dispatching aws requests, and an instance
poller, to poll instance status until a predicate returns true, and then deliver
a promise.

## Usage

Add the following to your dependencies:

```clj
[com.palletops/pallet-aws-ops "0.1.0"]
```

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
