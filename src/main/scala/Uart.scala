import Math.round
import spinal.core._
import spinal.lib.fsm._

class UartCore(
    len_data: Int,
    clock_rate: HertzNumber,
    bit_rate: HertzNumber
) extends Component {
  val io = new Bundle {
    val valid = in Bool
    val ready = out Bool
    val payload = in Bits (len_data bits)
    val txd = out Bool
  }

  // val period_timer = (clock_rate / bit_rate).rounded.toBigInt
  SpinalInfo("current MathContext = " + clock_rate.toBigDecimal.mc.toString)
  val period_timer = (clock_rate / bit_rate)
    .setScale(0, BigDecimal.RoundingMode.HALF_UP)
    .toBigInt
  SpinalInfo("period_timer = " + period_timer.toString)

  /*
   * Registers
   */
  val n_bits_sent = Reg(UInt(log2Up(len_data) bits)) // # bits already sent
  val ct_timer = Reg(UInt(log2Up(period_timer) bits))
  val data = Reg(Bits(len_data bits))

  /*
   * Clock Divider Counter
   */
  val ct_full: Bool = (ct_timer === period_timer - 1)
  ct_timer := ct_timer + 1
  when(ct_full) {
    ct_timer := 0
  }

  io.ready := False
  io.txd := True

  val fsm = new StateMachine {
    val idle = new State with EntryPoint
    val startbit = new State
    val sending = new State
    val stopbit = new State

    idle
      .whenIsActive {
        io.ready := True
        io.txd := True
        when(io.valid) {
          n_bits_sent := 0
          ct_timer := 0
          data := io.payload
          goto(startbit)
        }
      }

    startbit
      .whenIsActive {
        io.ready := False
        io.txd := False
        when(ct_full) {
          goto(sending)
        }
      }

    sending
      .whenIsActive {
        io.ready := False
        io.txd := data(0)
        when(ct_full) {
          data := (data >> 1).resized
          n_bits_sent := n_bits_sent + 1
          when(n_bits_sent === len_data - 1) {
            n_bits_sent := 0
            goto(stopbit)
          }
        }
      }

    stopbit
      .whenIsActive {
        io.ready := False
        io.txd := True
        when(ct_full) {
          goto(idle)
        }
      }
  }
}

object UartCoreVerilog {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new UartCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    )
  }
}
