import spinal.core._
import spinal.sim._
import spinal.core.sim._
import spinal.lib.master
import spinal.lib.com.uart._

package object mine2 {
  def my_assert(f: Boolean, msg: String): Unit = {
    assert(
      assertion = f,
      message = msg
    )
  }

  val PRD = BigDecimal(16e6 / 115200)
    .setScale(0, BigDecimal.RoundingMode.HALF_UP)
    .toInt
}

import mine2._

class SpinalUart extends Component {
  val io = new Bundle {
    val uart = master(Uart())
    val valid = in Bool
    val payload = in Bits(8 bits)
    val ready = out Bool
  }

  val uartCtrl = new UartCtrl()
  uartCtrl.io.config.setClockDivider(115.2 kHz, 16 MHz)
  uartCtrl.io.config.frame.dataLength := 7 // 8 bits
  uartCtrl.io.config.frame.parity := UartParityType.NONE
  uartCtrl.io.config.frame.stop := UartStopType.ONE
  uartCtrl.io.uart <> io.uart
  uartCtrl.io.write.valid := io.valid
  uartCtrl.io.write.payload := io.payload
  io.ready := uartCtrl.io.write.ready
}

object SpinalUartSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new SpinalUart(
      )
    ) { dut =>
      def wait(count: Int = 1) {
        dut.clockDomain.waitSampling(count)
      }

      def gen_uart_sig(data: Int, period: Int, assertion: Boolean): Unit = {
        // start-bit
        for (i <- 0 until period) {
          dut.io.uart.rxd #= false
          wait()
        }

        // character bits
        for (bit <- 0 to 7) {
          dut.io.uart.rxd #= (data & (1 << bit)) != 0
          for (i <- 0 until period) {
            wait()
          }
        }

        // stop-bit
        for (i <- 0 until period) {
          dut.io.uart.rxd #= true
          wait()
        }
      }

      dut.clockDomain.forkStimulus(period = 10)

      dut.io.valid #= false
      dut.io.payload #= 0x5a
      dut.io.uart.rxd #= true

      wait(5)

      dut.io.valid #= true

      while (dut.io.ready.toBoolean == false) {
        wait()
      }
      dut.io.valid #= false

      wait(600)

      gen_uart_sig(0x55, PRD, true)
      gen_uart_sig(0xaa, PRD, true)
      gen_uart_sig(0x5a, PRD, true)

      wait(10)
    }
  }
}
