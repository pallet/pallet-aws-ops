# pallet-aws-ops

Provide basic operations on AWS, on top of pallet [awaze][awaze].

Provides a forklift processor for dispatching aws requests, and an instance
poller, to poll instance status until a predicate returns true, and then deliver
a promise.

## Usage

Add the following to your dependencies:

```clj
[com.palletops/pallet-aws-ops "0.1.0"]
```

[API docs](http:/pallet.github.com/pallet-aws-ops/0.1/api/index.html).

[Annotated source](http:/pallet.github.com/pallet-aws-ops/0.1/uberdoc.html).

## License

Copyright © 2013, 2014 Hugo Duncan

Distributed under the Eclipse Public License.

[awaze]: https://github.com/pallet/awaze "Pallet AWS Client"
