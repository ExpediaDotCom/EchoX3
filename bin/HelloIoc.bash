#!/bin/bash

export set VER=0.1.0

java -classpath ./EchoX3-Client-$VER.jar:./EchoX3-Sample-Hello-$VER.jar:./log4j-1.2.17.jar com.expedia.echox3.container.ioc.HelloIoc


