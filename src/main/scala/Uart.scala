import spinal.core._
import spinal.lib.fsm._

class UartTxCore(
    len_data: Int,
    clock_rate: HertzNumber,
    bit_rate: HertzNumber
) extends Component {
  val io = new Bundle {
    val valid = in Bool
    val ready = out Bool
    val payload = in Bits (len_data bits)
    val tx_ready = out Bool
    val txd = out Bool
  }

  SpinalInfo("current MathContext = " + clock_rate.toBigDecimal.mc.toString)
  val period_timer = (clock_rate / bit_rate)
    .setScale(0, BigDecimal.RoundingMode.HALF_UP)
    .toBigInt
  SpinalInfo("period_timer = " + period_timer.toString)

  /*
   * Registers
   */
  val n_bits_sent = Reg(UInt(log2Up(len_data) bits)) init (0)
  val ct_timer = Reg(UInt(log2Up(period_timer) bits)) init (0)
  val data = Reg(Bits(len_data bits)) init (0)

  /*
   * Clock Divider Counter
   */
  val ct_full: Bool = (ct_timer === period_timer - 1)
  ct_timer := ct_timer + 1
  when(ct_full) {
    ct_timer := 0
  }

  io.ready := False
  io.tx_ready := False
  io.txd := True

  val fsm = new StateMachine {
    val idle = new State with EntryPoint
    val startbit = new State
    val sending = new State
    val stopbit = new State

    idle
      .whenIsActive {
        io.ready := False
        io.tx_ready := True
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
          io.ready := True
          goto(idle)
        }
      }
  }
}

class UartRxCore(
    len_data: Int,
    clock_rate: HertzNumber,
    bit_rate: HertzNumber
) extends Component {
  val io = new Bundle {
    val valid = out Bool
    val ready = in Bool
    val payload = out Bits (len_data bits)
    val rxd = in Bool
  }

  val period_timer = (clock_rate / bit_rate)
    .setScale(0, BigDecimal.RoundingMode.HALF_UP)
    .toBigInt

  /*
   * Registers
   */
  val n_bits_received = Reg(UInt(log2Up(len_data) bits)) init (0)
  val ct_timer = Reg(UInt(log2Up(period_timer) bits)) init (0)
  val data = Reg(Bits(len_data bits)) init (0)

  /*
   * Clock Divider Counter
   */
  val ct_full: Bool = (ct_timer === period_timer - 1)
  val ct_mid: Bool = (ct_timer === period_timer / 2 - 1)
  ct_timer := ct_timer + 1
  when(ct_full) {
    ct_timer := 0
  }

  io.valid := False
  io.payload := 0

  val fsm = new StateMachine {
    val idle = new State with EntryPoint
    val startbit = new State
    val receiving = new State
    val stopbit = new State

    idle
      .whenIsActive {
        when(io.rxd === False) { // found start-bit
          n_bits_received := 0
          ct_timer := 0
          goto(startbit)
        }
      }

    startbit
      .whenIsActive {
        when(ct_mid) { // (ct_full / 2)
          ct_timer := 0
          goto(receiving)
        }
      }

    receiving
      .whenIsActive {
        when(ct_full) {
          data := (data |>> 1) | (io.rxd.asBits << (len_data - 1))
          n_bits_received := n_bits_received + 1
          when(n_bits_received === len_data - 1) {
            goto(stopbit)
          }
        }
      }

    stopbit
      .whenIsActive {
        io.valid := True
        io.payload := data
        when(io.ready) {
          goto(idle)
        }
      }
  }
}

object UartTxCoreVerilog {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new UartTxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    )
  }
}

object UartRxCoreVerilog {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(
      new UartRxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    )
  }
}
