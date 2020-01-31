import spinal.core._

class UartApb3(
    len_data: Int,
    clock_rate: HertzNumber,
    bit_rate: HertzNumber
) extends Component {
  val io = new Bundle {
    val PADDR = in UInt (32 bits)
    val PSEL = in Bool
    val PENABLE = in Bool
    val PREADY = out Bool
    val PWRITE = in Bool
    val PWDATA = in Bits (32 bits)
    val PRDATA = out Bits (32 bits)
    val PSLVERROR = out Bool
    val txd = out Bool
  }

  /*
   * Register Address Definitions
   */
  val addr_base = 0x20000000
  val addr_txd = addr_base + 0
  val addr_status = addr_base + 4 // bit 0: tx_ready

  /*
   * Instantiation of a UART Core
   */
  val uart = new UartCore(
    len_data = 8,
    clock_rate = clock_rate,
    bit_rate = bit_rate
  )
  uart.io.txd <> io.txd

  /*
   * Write and Read Conditions
   */
  val write: Bool = io.PSEL && io.PENABLE && io.PWRITE
  val read: Bool = io.PSEL && io.PENABLE && !io.PWRITE

  /*
   * Main Part
   */
  io.PREADY := False
  io.PRDATA := 0
  io.PSLVERROR := False

  uart.io.valid := False
  uart.io.payload := 0

  when(io.PADDR === addr_txd && write) {
    io.PREADY := True
    uart.io.valid := True
    uart.io.payload := io.PWDATA.resized
  }.elsewhen(io.PADDR === addr_status && read) {
    io.PREADY := True
    io.PRDATA := uart.io.ready.asBits(1 bits).resized
  }
}
