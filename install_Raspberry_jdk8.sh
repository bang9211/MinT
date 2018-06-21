#!/bin/bash
export MINTPATH=/root/MinT
export MINTUSR=$MINTPATH/usr
export TARGET_MINT=/MinT
export TARGET_MINTUSR=$TARGET_MINT/usr
echo "Make MINT Location"
mkdir -p /MinT/usr
echo "install java"
tar xzvf $MINTUSR/jdk-8u121-linux-arm32-vfp-*.gz -C $TARGET_MINTUSR
echo "install wiringPi"
cp -rf $MINTUSR/wiringPi $TARGET_MINTUSR
cd $TARGET_MINTUSR/wiringPi
./build
echo "set bashrc"
cp -rf $MINTPATH/conf/bashrc_Raspberry_jdk8 ~/.bashrc
