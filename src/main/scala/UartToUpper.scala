import spinal.core._
import spinal.lib.fsm._

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
    var s10 = new State with EntryPoint
    var s20 = new State
    var s30 = new State
    var s40 = new State
    var s50 = new State
    var s60 = new State
    var s70 = new State
    var s80 = new State

    s10 // Checking rx_ready (1)
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s20)
      }

    s20 // Checking rx_ready (2)
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := False
        when(uart.io.PRDATA(1)) {
          goto(s30) // data is ready
        } otherwise {
          goto(s10) // not yet
        }
      }

    s30 // Reading received character (1)
      .whenIsActive {
        uart.io.PADDR := REG_READ
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s40)
      }

    s40 // Reading received character (2)
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
        goto(s50)
      }

    s50 // Writing character to send (1)
      .whenIsActive {
        uart.io.PADDR := REG_WRITE
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := True
        uart.io.PWDATA := data.resized
        goto(s60)
      }

    s60 // Writing character to send (2)
      .whenIsActive {
        uart.io.PADDR := REG_WRITE
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := True
        uart.io.PWDATA := data.resized
        goto(s70)
      }

    s70 // Checking tx_ready (1)
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := False
        uart.io.PWRITE := False
        goto(s80)
      }

    s80 // Checking tx_ready (2)
      .whenIsActive {
        uart.io.PADDR := REG_STATUS
        uart.io.PSEL := True
        uart.io.PENABLE := True
        uart.io.PWRITE := False
        when(uart.io.PRDATA(0)) {
          goto(s10) // tx_ready
        } otherwise {
          goto(s70) // not yet
        }
      }
  }
}
