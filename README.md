# classpath-verifier

`classpath-verifier` is a classpath verifier for JVM applications. Given a classpath and a set of
entry points, it verifies that all reachable code links and will not fail at runtime with a linkage
error.

## How to run

> 
> $ sbt
> 
> sbt:default-a6a307> classpath-verifier/run --classpath /path/to/classpath1:/path/to/classpath2.jar --entry foo.BMain
> 

## Support

Create a [new issue](https://github.com/twitter-incubator/classpath-verifier/issues/new) on GitHub.

## Contributing

We feel that a welcoming community is important and we ask that you follow Twitter's
[Open Source Code of Conduct](https://github.com/twitter/code-of-conduct/blob/master/code-of-conduct.md)
in all interactions with the community.


## Security Issues?

Please report sensitive security issues via Twitter's bug-bounty program
(https://hackerone.com/twitter) rather than GitHub.
>>>>>>> 2245025... Import classpath-verifier
