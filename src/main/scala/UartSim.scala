import spinal.core._
import spinal.sim._
import spinal.core.sim._

object UartTxCoreSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartTxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.io.payload #= 0
      dut.io.valid #= false

      for (idx <- 0 to 5000) {
        if (idx == 10) {
          dut.io.payload #= 123
          dut.io.valid #= true
        }

        if (dut.io.ready.toBoolean) {
          dut.io.valid #= false
        }

        dut.clockDomain.waitSampling()
      }
    }
  }
}

object UartRxCoreSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartRxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      val PRD = 139

      dut.io.ready #= false
      dut.io.rxd #= true

      for (idx <- 0 to 5000) {
        if (idx == 10) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 2) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 3) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 4) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 5) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 6) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 7) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 8) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 9) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 12) {
          dut.io.ready #= true
        }

        if (idx == 2400) {
          dut.io.ready #= false
        }
        if (idx == 2510) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 2) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 3) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 4) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 5) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 6) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 7) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 8) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 9) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 12) {
          dut.io.ready #= true
        }
        dut.clockDomain.waitSampling()
      }
    }
  }
}

object UartTxStringSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartTxString(
        str = "He", // "Hello World! ",
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      for (idx <- 0 to 5000) {
        dut.clockDomain.waitSampling()
      }
    }
  }
}

object UartApb3Sim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartApb3(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      dut.io.PADDR #= 0
      dut.io.PSEL #= false
      dut.io.PENABLE #= false
      dut.io.PWRITE #= false
      dut.io.PWDATA #= 0

      for (idx <- 0 to 5000) {
        /*
         * Writing to UartApb3 TxD register
         */
        if (idx == 10) {
          dut.io.PADDR #= 0x20000000
          dut.io.PSEL #= true
          dut.io.PWRITE #= true
          dut.io.PWDATA #= 0x5a
        } else if (idx == 11) {
          dut.io.PENABLE #= true
        } else if (idx == 12) {
          dut.io.PSEL #= false
          dut.io.PENABLE #= false
        }

        /*
         * Reading from UartApb3 status register (expecting to read 0)
         */
        if (idx == 20) {
          dut.io.PADDR #= 0x20000004
          dut.io.PSEL #= true
          dut.io.PWRITE #= false
        } else if (idx == 21) {
          dut.io.PENABLE #= true
        } else if (idx == 22) {
          dut.io.PSEL #= false
          dut.io.PENABLE #= false
        }

        /*
         * Reading from UartApb3 status register (expecting to read 1)
         */
        if (idx == 1620) {
          dut.io.PADDR #= 0x20000004
          dut.io.PSEL #= true
          dut.io.PWRITE #= false
        } else if (idx == 1621) {
          dut.io.PENABLE #= true
        } else if (idx == 1622) {
          dut.io.PSEL #= false
          dut.io.PENABLE #= false
        }

        dut.clockDomain.waitSampling()
      }
    }
  }
}
