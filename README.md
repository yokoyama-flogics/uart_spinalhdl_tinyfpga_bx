# Beginner's UART by SpinalHDL for TinyFPGA BX

This is my second [SpinalHDL](https://github.com/SpinalHDL) design for
[TinyFPGA BX](https://tinyfpga.com/bx/guide.html).

## Tutorial by Blog

A SpinalHDL tutorial for software designer (written in Japanese) is provided
on my work blog.

- [Part 1: How Software Programmers Design a UART](https://flogics.com/wp/ja/2020/01/spinalhdl-uart-part1/)
- [Part 2: Improving the UART (Elaborating Design and Adding String Transmission)](https://flogics.com/wp/ja/2020/01/spinalhdl-uart-part2/)
- [Part 3: Adding APB3 Bus to the UART](https://flogics.com/wp/ja/2020/02/spinalhdl-uart-part3/)
- [Also Designed UART Rx](https://flogics.com/wp/ja/2020/02/spinalhdl-uart-rx/)
- [Other Writings](https://flogics.com/wp/ja/category/spinalhdl/)

## Required Hardware

- TinyFPGA BX board

  - [TinyFPGA](https://tinyfpga.com/)
  - [Crowd Supply](https://www.crowdsupply.com/tinyfpga/tinyfpga-bx)

## Required Software

- [sbt](https://www.scala-sbt.org/)

  Please refer
  [this page](https://spinalhdl.github.io/SpinalDoc/spinal_getting_started/)
  to install sbt.

- [Yosys](http://www.clifford.at/yosys/)

  [This page](http://www.clifford.at/icestorm/) is helpful to install Yosys.

- [IceStorm Tools](https://github.com/cliffordwolf/icestorm)

   Please refer [this page](http://www.clifford.at/icestorm/) again.

- [nextpnr](https://github.com/YosysHQ/nextpnr)

  The official GitHub site and also
  [this page](http://www.clifford.at/icestorm/) is helpful.

- [tinyprog](https://github.com/tinyfpga/TinyFPGA-Bootloader/tree/master/programmer)

  This [TinyFPGA BX User Guide](https://tinyfpga.com/bx/guide.html) is a
  good starting point to begin TinyFPGA BX.

- OPTIONAL: [ice40_viewer](https://github.com/knielsen/ice40_viewer)

  It generates layout views of your design, but it is optional.

## Building by make

You can build ```UartToUpper_TinyFPGA_BX``` which runs TinyFPGA BX, and receive UART characters (from pin H2) at 115.2kbps and re-transmit them (from pin H9) after converting to upper cases if the characters are lower case.

![UartToUpper_TinyFPGA_BX](image/uart_to_upper.svg)

NOTE: If you run ice40_viewer, please modify 'ICEVIEW' line in the Makefile
according to your installation.

- ```make```: Building bit-stream file for TinyFPGA BX

- ```make html```: Generating a layout view by ice40_viewer

- ```make upload```: Uploading a bit-stream file to TinyFPGA BX
