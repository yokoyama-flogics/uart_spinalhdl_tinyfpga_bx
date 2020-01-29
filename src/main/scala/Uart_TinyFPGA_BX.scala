import spinal.core._

class Uart_TinyFPGA_BX extends Component {
  val io = new Bundle {
    val CLK = in Bool
    val TXD = out Bool
    val USBPU = out Bool
  }

  /*
   * Refer https://wolfgang-jung.net/posts/2018-07-19-spinalhdl/ and
   * https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Structuring/clock_domain.html
   */
  val coreClockDomain = ClockDomain(
    clock = io.CLK,
    frequency = FixedFrequency(16 MHz),
    config = ClockDomainConfig(
      resetKind = BOOT
    )
  )

  val coreArea = new ClockingArea(coreClockDomain) {
    io.USBPU := False

    val uart = new UartCore(
      len_data = 8,
      clock_rate = ClockDomain.current.frequency.getValue,
      bit_rate = 115200 Hz
    )
    uart.io.txd <> io.TXD
    uart.io.valid <> True
    uart.io.payload <> 65
  }
}

object Uart_TinyFPGA_BX {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new Uart_TinyFPGA_BX)
  }
}
