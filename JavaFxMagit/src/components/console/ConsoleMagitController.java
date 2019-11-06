package components.console;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.*;

public class ConsoleMagitController {
    private TextArea m_Console;
    private PrintStream m_PrintStream;
    private ConsoleInput m_InputStream;

    public ConsoleMagitController(TextArea i_Console) {
        m_Console = i_Console;
        m_PrintStream = new PrintStream(new ConsoleOutput(i_Console));
        m_InputStream =  new ConsoleInput();

        System.setOut(m_PrintStream);
        System.setErr(m_PrintStream);
        System.setIn(m_InputStream);
    }

    public void SetBuffer(String i_Buffer) {
        m_InputStream.SetBuffer(i_Buffer);
    }


    public class ConsoleOutput extends OutputStream {
        private TextArea console;

        public ConsoleOutput(TextArea console) {
            this.console = console;
        }

        public void appendText(String valueOf) {
            Platform.runLater(() -> console.appendText(valueOf));
        }

        @Override
        public void write(int b) throws IOException {
            appendText(String.valueOf((char)b));
        }
    }

    public class ConsoleInput extends InputStream {
        private byte[] m_Buffer;
        private int m_Index;
        private boolean isValueSet;
        private int m_Counter;

        public ConsoleInput() {
            m_Index = 0;
            m_Buffer = null;
            isValueSet = false;
            m_Counter = 0;
        }

        public synchronized void SetBuffer(String i_Buffer) {
            isValueSet = true;
            m_Buffer = i_Buffer.getBytes();
            notifyAll();
        }

        @Override
        public synchronized int read() throws IOException {
            while (!isValueSet) {
                try {
                    wait();
                } catch (InterruptedException ignored) {}
            }

            byte result;

            if(m_Buffer == null) {
                m_Counter++;

                if(m_Counter == 2) {
                    isValueSet = false;
                    m_Counter = 0;
                }

                return -1;
            }
            else {
                if(m_Buffer.length == 0) {
                    result = 0;
                }
                else {
                    result = m_Buffer[m_Index++];
                }

                if (m_Index >= m_Buffer.length) {
                    m_Buffer = null;
                    m_Index = 0;
                }
            }

            return result;
        }
    }
}
