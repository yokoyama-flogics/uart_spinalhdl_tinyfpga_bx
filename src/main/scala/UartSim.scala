import spinal.core._
import spinal.sim._
import spinal.core.sim._

object UartCoreSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartCore(
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
        } else if (idx == 15) {
          dut.io.payload #= 0
          dut.io.valid #= false
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
        str = "Hello World! ",
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
