# Composer

Composer is a system for exploring rules in Western music. It uses
TouchOSC for interacting with the rules, core.logic for composing
melodies, Overtone for playing melodies and core.async for wiring the
system.

## Running the system

Overtone can be a bit, eh, difficult when using dynamic reloading of
namespaces etc.. To get the system up and running, you should boot the
REPL, evaluate the commented sections of the `overtone.clj` namespace
and then run `user/go`.

To interact with the running system, you should install
[TouchOSC](http://hexler.net/software/touchosc) on your phone, load up
the interface from the `touchosc/` folder onto your phone using the
TouchOSC builder, and connect to the running instance. Again, this
requires a bit of work and having an interface straight out of Composer
running on your box would be nice addition.

If you're stuck, don't hesitate to contact me on twitter
(`@tgkristensen`).

## Presentations

Composer has been presented at the following conferences:

- FARM 2014 Workshop at ICFP,
  Gothenburg. [Slides](http://functional-art.org/2014/ThomasGKristensen.pdf),
  [video](https://www.youtube.com/watch?v=Y12tGSrJjc4) and
  [paper](http://dl.acm.org/citation.cfm?id=2633646). The paper and talk
  is concerned with the foundation of automated musical composition, the
  system design aimed at reponsiveness and some of the problems
  associated with using logic programming for music composition.
- FP Days 2014, London. [Slides](Thomas Kristensen FPDays 2014.pdf),
  video to appear online mid 2015. The presentation is concerned with
  how a high degree of decoupling is achieved in Composer, and how this
  relates to more traditional concepts of decoupling.

There is also [an early video](http://vimeo.com/55677313) of some of the
work that lay the foundation of the Composer system.
