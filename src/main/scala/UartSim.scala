import spinal.core._
import spinal.sim._
import spinal.core.sim._

object UartTxCoreSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartTxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      def wait(count: Int = 1) {
        dut.clockDomain.waitSampling(count)
      }

      def my_assert(f: Boolean, msg: String): Unit = {
        assert(
          assertion = f,
          message = msg
        )
      }

      def test_uart_tx(data: Int, period: Int, assertion: Boolean): Unit = {
        // Check start-bit
        for (i <- 0 until period) {
          my_assert(
            assertion && dut.io.txd.toBoolean == false,
            "[start-bit] i = " + i.toString
          )
          wait()
        }

        // Check every bit in the character
        for (bit <- 0 to 7) {
          val bit_bool = (data & (1 << bit)) != 0
          for (i <- 0 until period) {
            my_assert(
              assertion && dut.io.txd.toBoolean == bit_bool,
              "[bit " + bit.toString + "] i = " + i.toString
            )
            wait()
          }
        }

        // Check stop-bit
        for (i <- 0 until period) {
          my_assert(
            assertion && dut.io.txd.toBoolean == true,
            "[stop-bit] i = " + i.toString
          )

          if (i != period - 1)
            wait() // doesn't wait after the last assertion
        }
      }

      dut.clockDomain.forkStimulus(period = 10)

      val PRD = BigDecimal(16e6 / 115200)
        .setScale(0, BigDecimal.RoundingMode.HALF_UP)
        .toInt
      val DATA = 123

      // Initialize inputs
      dut.io.payload #= 0
      dut.io.valid #= false

      // Check the initial outputs
      wait(2)
      my_assert(
        dut.io.tx_ready.toBoolean == true && dut.io.ready.toBoolean == false,
        msg = "initial outputs"
      )

      // Start transmission
      dut.io.payload #= DATA
      dut.io.valid #= true

      wait(2)
      my_assert(
        dut.io.tx_ready.toBoolean == false && dut.io.ready.toBoolean == false,
        msg = "tx_ready goes false"
      )

      test_uart_tx(
        data = DATA,
        period = PRD,
        dut.io.tx_ready.toBoolean == false && dut.io.ready.toBoolean == false
      )

      my_assert(
        dut.io.tx_ready.toBoolean == false && dut.io.ready.toBoolean == true,
        msg = "io.ready"
      )

      dut.io.valid #= false

      wait()
      my_assert(
        dut.io.tx_ready.toBoolean == true && dut.io.ready.toBoolean == false,
        msg = "back to idle"
      )

      wait(10)
    }
  }
}

object UartRxCoreSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartRxCore(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      val PRD = 139
      var ready_55 = false
      var ready_aa = false
      var ready_5a = false

      dut.io.rxd #= true

      for (idx <- 0 to 6000) {
        if (idx == 10) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 2) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 3) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 4) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 5) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 6) {
          dut.io.rxd #= false
        }
        if (idx == 10 + PRD * 7) {
          dut.io.rxd #= true
        }
        if (idx == 10 + PRD * 8) {
          dut.io.rxd #= false
          ready_55 = true
        }
        if (idx == 10 + PRD * 9) {
          dut.io.rxd #= true
        }

        if (ready_55 == true && dut.io.valid.toBoolean) {
          ready_55 = false
          assert(
            dut.io.payload.toLong == 0x55,
            message = "idx = " + idx.toString
              + ", PRDATA = 0x" + dut.io.payload.toLong.toHexString
          )
        }

        if (idx == 2510) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 2) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 3) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 4) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 5) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 6) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 7) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 8) {
          dut.io.rxd #= true
          ready_aa = true
        }
        if (idx == 2510 + PRD * 9) {
          dut.io.rxd #= true
        }

        if (ready_aa == true && dut.io.valid.toBoolean) {
          ready_aa = false
          assert(
            dut.io.payload.toLong == 0xaa,
            message = "idx = " + idx.toString
              + ", PRDATA = 0x" + dut.io.payload.toLong.toHexString
          )
        }

        if (idx == 2510 + PRD * 10) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 11) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 12) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 13) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 14) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 15) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 16) {
          dut.io.rxd #= false
        }
        if (idx == 2510 + PRD * 17) {
          dut.io.rxd #= true
        }
        if (idx == 2510 + PRD * 18) {
          dut.io.rxd #= false
          ready_5a = true
        }
        if (idx == 2510 + PRD * 19) {
          dut.io.rxd #= true
        }

        if (ready_5a == true && dut.io.valid.toBoolean) {
          ready_5a = false
          assert(
            dut.io.payload.toLong == 0x5a,
            message = "idx = " + idx.toString
              + ", PRDATA = 0x" + dut.io.payload.toLong.toHexString
          )
        }

        dut.clockDomain.waitSampling()
      }
    }
  }
}

object UartTxStringSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartTxString(
        str = "He", // "Hello World! ",
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      //Fork a process to generate the reset and the clock on the dut
      dut.clockDomain.forkStimulus(period = 10)

      for (idx <- 0 to 5000) {
        dut.clockDomain.waitSampling()
      }
    }
  }
}

object UartApb3Sim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartApb3(
        len_data = 8,
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      def wait(count: Int = 1) {
        dut.clockDomain.waitSampling(count)
      }

      def my_assert(f: Boolean, msg: String): Unit = {
        assert(
          assertion = f,
          message = msg
        )
      }

      def write(addr: BigInt, data: BigInt): Unit = {
        dut.io.PADDR #= addr
        dut.io.PSEL #= true
        dut.io.PENABLE #= false
        dut.io.PWRITE #= true
        dut.io.PWDATA #= data

        wait()
        dut.io.PENABLE #= true

        wait()
        dut.io.PSEL #= false
        dut.io.PENABLE #= false
      }

      def read_assert(addr: BigInt, expecting: BigInt, msg: String): Unit = {
        dut.io.PADDR #= addr
        dut.io.PSEL #= true
        dut.io.PENABLE #= false
        dut.io.PWRITE #= false

        wait()
        dut.io.PENABLE #= true

        wait()
        my_assert(
          dut.io.PRDATA.toBigInt == expecting,
          msg + " expected PRDATA = " + expecting.toString
            + ", actual PRDATA = " + dut.io.PRDATA.toBigInt.toString
        )

        dut.io.PSEL #= false
        dut.io.PENABLE #= false
      }

      dut.clockDomain.forkStimulus(period = 10)

      val PRD = 139 // bit rate period cycles
      val REG_WRITE = 0x20000000
      val REG_READ = 0x20000004
      val REG_STATUS = 0x20000008

      /*
       * Initialize inputs
       */
      dut.io.PADDR #= 0
      dut.io.PSEL #= false
      dut.io.PENABLE #= false
      dut.io.PWRITE #= false
      dut.io.PWDATA #= 0
      dut.io.rxd #= true

      wait(2)
      write(REG_WRITE, 0x5a)

      wait(10)
      read_assert(REG_STATUS, expecting = 0, "[xmitting]")

      wait(PRD * 11)
      read_assert(REG_STATUS, expecting = 1, "[xmit done]")

      /*
       * Transmitting a character
       */
      wait()
      dut.io.rxd #= false // start-bit
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true // stop-bit

      wait(5)
      read_assert(REG_STATUS, expecting = 3, "[recv done]")

      wait(5)
      read_assert(REG_READ, expecting = 0x55, "[recv data]")

      wait(5)
      read_assert(REG_STATUS, expecting = 1, "[read buf empty]")
    }
  }
}

object UartToUpperSim {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.doSim(
      new UartToUpper(
        clock_rate = 16 MHz,
        bit_rate = 115200 Hz
      )
    ) { dut =>
      def wait(count: Int = 1) {
        dut.clockDomain.waitSampling(count)
      }

      def my_assert(f: Boolean, msg: String): Unit = {
        assert(
          assertion = f,
          message = msg
        )
      }

      dut.clockDomain.forkStimulus(period = 10)

      val PRD = 139 // bit rate period cycles

      /*
       * Initialize inputs
       */
      dut.io.rxd #= true

      /*
       * Transmitting a character
       */
      wait(2)
      dut.io.rxd #= false // start-bit
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true // stop-bit

      /*
       * Transmitting a character
       */
      wait(PRD * 15)
      dut.io.rxd #= false // start-bit
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= true
      wait(PRD)
      dut.io.rxd #= false
      wait(PRD)
      dut.io.rxd #= true // stop-bit

      wait(PRD * 15)
    }
  }
}
