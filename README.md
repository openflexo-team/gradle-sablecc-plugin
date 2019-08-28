Introduction

This is a gradle plugin which creates parsers using
[SableCC](http://sablecc.org/), clearly the best parser-generator known
to man. Why is it the best? Because it supports automatic CST-to-AST
transformation, emits all the visitor patterns and analysis helpers
you will ever need, and is LR, not LL(k).

In the beginning it was a simple fork from [shevek/gradle-sablecc-plugin](https://github.com/shevek/gradle-sablecc-plugin) but then a complete rewrite happened based on the current ANTLR plugin from the gradle code.

# Usage

To apply a default configuration which transforms src/main/sablecc
into build/generated-sources/sablecc:

	buildscript {
       repositories {
         maven { url uri('https://maven.openflexo.org/artifactory/openflexo-deps/') }
       }
       dependencies {
         classpath 'org.openflexo:gradle-sablecc-plugin:[1.2.0,)'
       }
     }

	apply plugin: 'sablecc'

Notice for the moment it is not configurable... Help is welcome.
