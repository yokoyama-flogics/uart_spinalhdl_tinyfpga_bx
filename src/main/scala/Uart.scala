import spinal.core._
import spinal.lib.fsm._

class UartCore(len_data: Int) extends Component {
  val io = new Bundle {
    val valid = in Bool
    val ready = out Bool
    val payload = in Bits (len_data bits)
    val txd = out Bool
  }

  val period_timer = 16 * 1000 * 1000 / 115200

  val n_bits_sent = Reg(UInt(log2Up(len_data) bits)) // # bits already sent
  // val ct_timer = Reg(UInt((16 * 1000 * 1000 / 115200) bits))
  val ct_timer = Reg(UInt(log2Up(period_timer) bits))
  val data = Reg(Bits(len_data bits))

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
        ct_timer := ct_timer + 1
        io.ready := False
        io.txd := False
        when(ct_timer === period_timer - 1) {
          ct_timer := 0
          goto(sending)
        }
      }

    sending
      .whenIsActive {
        ct_timer := ct_timer + 1
        io.ready := False
        io.txd := data(0)
        when(ct_timer === period_timer - 1) {
          ct_timer := 0
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
        ct_timer := ct_timer + 1
        io.ready := False
        io.txd := True
        when(ct_timer === period_timer - 1) {
          ct_timer := 0
          goto(idle)
        }
      }
  }
}

object UartCoreVerilog {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new UartCore(len_data = 8))
  }
}
