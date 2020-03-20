import spinal.core._
import spinal.lib.fsm._
import spinal.lib.Counter

class UartToUpper_TinyFPGA_BX extends Component {
  val io = new Bundle {
    val CLK = in Bool
    val RXD = in Bool
    val TXD = out Bool
    val USBPU = out Bool
  }
  /* If you like synchronous reset,
   *
  val boot_clock_domain = ClockDomain(
    clock = io.CLK,
    frequency = FixedFrequency(16 MHz),
    config = ClockDomainConfig(
      resetKind = BOOT
    )
  )

  val boot_area = new ClockingArea(boot_clock_domain) {
    val ct = Counter(16)
    val delayed_reset = ct.willOverflow
    ct.increment()
  }
   */

  val core_clock_domain = ClockDomain(
    clock = io.CLK,
    frequency = FixedFrequency(16 MHz),
    // reset = boot_area.delayed_reset, // if you like synchronous reset
    config = ClockDomainConfig(
      resetKind = BOOT // change also here...
    )
  )

  val core_area = new ClockingArea(core_clock_domain) {
    io.USBPU := False

    val uart_to_upper = new UartToUpper(
      clock_rate = ClockDomain.current.frequency.getValue,
      bit_rate = 115200 Hz
    )
    uart_to_upper.io.txd <> io.TXD
    uart_to_upper.io.rxd <> io.RXD
  }
}

object UartToUpper_TinyFPGA_BX {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new UartToUpper_TinyFPGA_BX)
  }
}
