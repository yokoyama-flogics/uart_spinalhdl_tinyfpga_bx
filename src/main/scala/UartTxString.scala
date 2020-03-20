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
