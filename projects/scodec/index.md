---
layout: index
title: "scodec"

meta:
  nav: projects
  canonical: "projects/scodec"
  pygments: true
---

<div class="jumbotron">
  <h1>scodec</h1>
  <p class="lead">Combinator library for working with binary data</p>
</div>

scodec is a suite of libraries for working with binary data. Support ranges from simple, performant data structures for working with bits and bytes to streaming encoding and decoding of common protocols such as tcpdump captures and MPEG2 transport streams.

```scala
scala> import scodec.bits._
import scodec.bits._

scala> val otp = hex"54686973206973206e6f74206120676f6f642070616420746f2075736521".bits
otp: scodec.bits.BitVector = BitVector(240 bits, 0x54686973206973206e6f74206120676f6f642070616420746f2075736521)

scala> val bits = hex"746be39ece241e0da28b7acd4fad63632249ec5e2e402d5a0b2cd95d0a05".bits
bits: scodec.bits.BitVector = BitVector(240 bits, 0x746be39ece241e0da28b7acd4fad63632249ec5e2e402d5a0b2cd95d0a05)

scala> val decoded = (bits ^ otp) rotateLeft 3
decoded: scodec.bits.BitVector = BitVector(240 bits, 0x001c576f726b696e6720776974682062696e617279206973206561737921)

scala> import scodec.codecs._
import scodec.codecs._

scala> val msg = variableSizeBytes(uint16, utf8).decodeValidValue(decoded)
msg: String = Working with binary is easy!
````

## Modules

 - [scodec-bits](https://github.com/scodec/scodec-bits) - Zero dependency library that provides persistent data structures,
   `BitVector` and `ByteVector`, for working with binary.
 - [scodec-core](https://github.com/scodec/scodec) - Combinator based library for encoding/decoding values to/from binary,
   including automatic binding to case classes powered by shapeless.
 - [scodec-stream](https://github.com/scodec/scodec-stream) - Binding between scodec-core and scalaz-stream that enables streaming
   encoding/decoding.
 - [scodec-protocols](https://github.com/scodec/scodec-protocols) - Library of general purpose implementations of common
   protocols.

The README file in each project has more information specific to the module. Besides scodec-bits, each module listed builds on the
module above it in the list.

## Articles / Presentations

 - [Blog series on development of scodec-core](http://mpilquist.github.io/blog/categories/scodec/)

## Getting in touch

 - [scodec Google Group](https://groups.google.com/forum/#!forum/scodec)
 - Twitter: [@mpilquist](https://twitter.com/mpilquist)

