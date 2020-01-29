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
