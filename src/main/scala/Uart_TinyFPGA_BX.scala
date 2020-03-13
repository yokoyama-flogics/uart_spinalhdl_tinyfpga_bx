import spinal.core._
import spinal.lib.fsm._
import spinal.lib.Counter

class UartTxString(
    str: String,
    clock_rate: HertzNumber,
    bit_rate: HertzNumber,
    delayed_start: Boolean = false
) extends Component {
  val io = new Bundle {
    val txd = out Bool
  }

  val chr_size = 8 // bits

  /*
   * References:
   *   https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Getting%20Started/presentation.html (class SinusGenerator)
   *   https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Data%20types/bits.html
   *   https://stackoverflow.com/questions/5052042/how-to-split-strings-into-characters-in-scala
   *   https://groups.google.com/forum/#!topic/scala-user/po68d2V0szM
   */
  def chrTable =
    (0 until str.length).map(i => {
      B(str.toList(i).toInt, chr_size bits)
    })
  SpinalInfo("chrTable: " + chrTable.toString)
  val rom_str = Mem(Bits(chr_size bits), initialContent = chrTable)
  val n_char_sent = Reg(UInt(log2Up(str.length) bits)) init (0)

  val uart = new UartTxCore(
    len_data = chr_size,
    clock_rate = clock_rate,
    bit_rate = bit_rate
  )
  uart.io.txd <> io.txd

  uart.io.valid := False
  uart.io.payload := rom_str.readAsync(n_char_sent)

  val fsm = new StateMachine {
    var waiting: State = null
    var active: State = null
    val ct_waiting = Counter(clock_rate.toBigDecimal.rounded.toBigInt)

    if (delayed_start) {
      waiting = new State with EntryPoint
      active = new State
    } else {
      waiting = new State
      active = new State with EntryPoint
    }

    /*
    init_no_more_used
      .whenIsActive {
        uart.io.valid := False
        uart.io.payload := 0
        n_char_sent := 0
        ct_init.increment()
        when(ct_init.willOverflow) {
          goto(waiting)
        }
      }
     */

    waiting
      .whenIsActive {
        uart.io.valid := False
        ct_waiting.increment()
        when(ct_waiting.willOverflow) {
          goto(active)
        }
      }

    active
      .whenIsActive {
        uart.io.valid := True
        when(uart.io.ready) {
          n_char_sent := n_char_sent + 1
          when(n_char_sent === str.length - 1) {
            // XXX uart.io.valid := False (this causes combinational loop)
            n_char_sent := 0
            goto(waiting)
          }
        }
      }
  }
}

class UartToUpper(
    clock_rate: HertzNumber,
    bit_rate: HertzNumber
) extends Component {
  val io = new Bundle {
    val rxd = in Bool
    val txd = out Bool
  }

  val chr_size = 8 // bits
  val REG_WRITE = 0x20000000
  val REG_READ = 0x20000004
  val REG_STATUS = 0x20000008

  val data = Reg(Bits(chr_size bits)) init (0)

  val uart = new UartApb3(
    len_data = chr_size,
    clock_rate = clock_rate,
    bit_rate = bit_rate
  )
  uart.io.txd <> io.txd
  uart.io.rxd <> io.rxd

  uart.io.PADDR := 0
  uart.io.PSEL := False
  uart.io.PENABLE := False
  uart.io.PWRITE := False
  uart.io.PWDATA := 0

  val fsm = new StateMachine {
    var s0 = new State with EntryPoint
    var s1 = new State
    var s1a = new State
    var s1b = new State
    var s2 = new State
    var s3 = new State
    var s4 = new State
    var s5 = new State

    s0
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s1)
      }

    s1
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := False
        when(uart.io.PRDATA(1)) {
          goto(s1a)
        } otherwise {
          goto(s0)
        }
      }

    s1a
      .whenIsActive {
        uart.io.PADDR := REG_READ
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s1b)
      }

    s1b
      .whenIsActive {
        uart.io.PADDR := REG_READ
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := False

        var v: UInt = uart.io.PRDATA.resize(chr_size).asUInt
        when(v >= 0x61 && v <= 0x7a) {
          data := (v - 0x20).asBits
        } otherwise {
          data := v.asBits
        }
        goto(s2)
      }

    s2
      .whenIsActive {
        uart.io.PADDR := REG_WRITE
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := True
        uart.io.PWDATA := data.resized
        goto(s3)
      }

    s3
      .whenIsActive {
        uart.io.PADDR := REG_WRITE
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := True
        uart.io.PWDATA := data.resized
        goto(s4)
      }

    s4
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s5)
      }

    s5
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := False
        when(uart.io.PRDATA(0)) {
          goto(s0)
        } otherwise {
          goto(s4)
        }
      }
  }
}

class Uart_TinyFPGA_BX extends Component {
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

/*
    val uart_tx_str = new UartTxString(
      str = "Hello World! ",
      clock_rate = ClockDomain.current.frequency.getValue,
      bit_rate = 115200 Hz,
      delayed_start = true
    )
    uart_tx_str.io.txd <> io.TXD
*/

    val uart_to_upper = new UartToUpper(
      clock_rate = ClockDomain.current.frequency.getValue,
      bit_rate = 115200 Hz
    )
    uart_to_upper.io.txd <> io.TXD
    uart_to_upper.io.rxd <> io.RXD
  }
}

object Uart_TinyFPGA_BX {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new Uart_TinyFPGA_BX)
  }
}
