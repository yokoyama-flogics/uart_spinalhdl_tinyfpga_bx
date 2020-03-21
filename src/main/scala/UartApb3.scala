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
    val rxd = in Bool
  }

  /*
   * Register Address Definitions
   */
  val addr_base = 0x20000000
  val addr_txd = addr_base + 0
  val addr_rxd = addr_base + 4
  val addr_status = addr_base + 8 // bit 1: rx_ready, 0: tx_ready

  val rx_data = Reg(Bits(len_data bits)) init (0)
  val rx_ready = Reg(Bool) init (False)

  /*
   * Instantiation of UART Cores
   */
  val uart_tx = new UartTxCore(
    len_data = 8,
    clock_rate = clock_rate,
    bit_rate = bit_rate
  )
  uart_tx.io.txd <> io.txd

  val uart_rx = new UartRxCore(
    len_data = 8,
    clock_rate = clock_rate,
    bit_rate = bit_rate
  )
  uart_rx.io.rxd <> io.rxd

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

  uart_tx.io.valid := False
  uart_tx.io.payload := 0

  when(uart_rx.io.valid) {
    rx_data := uart_rx.io.payload
    rx_ready := True
  }

  when(io.PADDR === addr_txd && write) {
    io.PREADY := True
    uart_tx.io.valid := True
    uart_tx.io.payload := io.PWDATA.resized
  }.elsewhen(io.PADDR === addr_rxd && read) {
    io.PREADY := True
    io.PRDATA := rx_data.resized
    rx_ready := False
  }.elsewhen(io.PADDR === addr_status && read) {
    io.PREADY := True
    io.PRDATA := (
      1 -> rx_ready,
      0 -> uart_tx.io.tx_ready,
      default -> False
    )
  }
}
