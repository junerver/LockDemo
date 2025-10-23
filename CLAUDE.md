# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LockDemo is a comprehensive Android door lock control application written in Kotlin and Java. The
application provides serial communication functionality for controlling door lock control boards
with support for individual and batch lock operations, LED control, status monitoring, and real-time
response data display. The system is designed specifically for tablet devices with optimized layouts
for efficient space utilization.

## Key Features

- **Serial Communication**: RS485 serial port communication with door lock control boards
- **Lock Control**: Support for 7 door locks (1-7) with individual and batch operations
- **Multiple Operation Modes**: Sequential and simultaneous lock opening capabilities
- **LED Control**: Individual LED flashing functionality for each channel
- **Status Monitoring**: Real-time lock status query and display
- **Channel Management**: Channel keep-open and close operations
- **Auto-Connect**: Automatic serial port connection on application startup
- **Real-time Response**: Live response data display with timestamps
- **Multi-Activity Architecture**: Dedicated activities for different lock operations
- **Tablet Optimization**: UI optimized for tablet devices with efficient space utilization

## Lock Control Protocol

The application implements a comprehensive lock control board protocol supporting instructions
0x80-0x89:

- **0x80**: Open multiple locks simultaneously
- **0x81**: Channel LED flashing
- **0x82**: Open single lock
- **0x83**: Query single lock status
- **0x84**: Query all locks status
- **0x85**: Lock status active report (used by lock control board to report status when door lock opens/closes)
- **0x86**: Open all locks sequentially
- **0x87**: Open multiple locks sequentially
- **0x88**: Channel keep open
- **0x89**: Close channel

## Data Handling

- **Fragmented Data Support**: Proper buffering and validation for incomplete serial data packets
- **Thread-Safe Operations**: Synchronized data handling for concurrent access
- **JSON Response Parsing**: Structured response data parsing with error handling
- **Checksum Validation**: XOR-based data integrity verification


## Project Structure

- **app/src/main/java/xyz/junerver/android/lockdemo/**: Main application source code
  - `MainActivity.kt`: Main activity with serial connection management, auto-connect, and
    comprehensive lock control UI
  - `SequentialOpenActivity.kt`: Secondary activity for sequential/simultaneous lock opening with
    mode switching
  - `LedFlashActivity.kt`: LED control activity for individual channel LED flashing operations
  - `LockCtlBoardUtil.java`: Core serial communication and lock control utility (singleton pattern)
  - `LockCtlBoardCmdHelper.java`: Command construction and response parsing helper for all protocol
    instructions
- **app/src/test/**: Unit tests
  - `ExampleUnitTest.kt`: Comprehensive unit tests for all lock control instructions (0x80-0x89)
- **app/src/androidTest/**: Instrumented tests
- **app/src/main/res/**: Android resources (layouts, values, drawables)
  - `activity_main.xml`: Main layout optimized for tablets with 4-column button grid and integrated
    response area
  - `activity_sequential_open.xml`: Sequential/simultaneous lock opening layout with mode toggle
  - `activity_led_flash.xml`: LED control layout with individual channel buttons
- **gradle/**: Gradle configuration including version catalog (libs.versions.toml)

## Architecture Notes

- Multi-activity architecture with dedicated screens for different lock operations
- Kotlin and Java-based project with Java 11 compatibility
- Uses LinearLayout and GridLayout for optimized tablet layouts
- Implements serial port communication with proper data buffering
- Singleton pattern for lock control utility with thread-safe operations
- Handler-based UI updates for real-time serial data display
- Comprehensive error handling and connection management
- JSON-based response parsing with structured data handling

## Development Configuration

- **Compile SDK**: 35
- **Min SDK**: 24
- **Target SDK**: 35
- **Kotlin Version**: 2.0.21
- **Java Compatibility**: Version 11
- **Application ID**: xyz.junerver.android.lockdemo

## Dependencies

Core Android dependencies managed through version catalog:

- AndroidX Core KTX, AppCompat, Activity
- Material Design components
- Serial communication library (com.kongqw.serialportlibrary)
- JSON parsing library (com.google.code.gson:gson)
- Standard testing libraries (JUnit, Espresso)

## Serial Communication

- **Device Path**: /dev/ttyS4
- **Baud Rate**: 9600
- **Protocol**: Custom lock control board protocol with frame validation
- **Communication**: RS485 serial communication
- **Data Format**: Binary with XOR checksum validation
- **Response Format**: JSON-structured response data

## Lock Status Definitions

- **0x00**: Lock open
- **0x01**: Lock closed
- **0xFF**: Error/unknown status

## Key Methods

### LockCtlBoardUtil

- `openSerialPort()`: Establish serial connection
- `closeSerialPort()`: Close serial connection and clear buffers
- `openSingleLock(int)`: Open individual lock
- `openMultipleLocksSimultaneously(int...)`: Open multiple locks simultaneously
- `openMultipleLocksSequentially(int...)`: Open multiple locks sequentially (0x87 instruction)
- `openAllLocksSequentially()`: Open all locks sequentially (0x86 instruction)
- `flashLockLed(int)`: Flash LED for specific channel
- `keepChannelOpen(int, long)`: Keep channel open for specified duration
- `closeChannel(int)`: Close specific channel
- `getSingleLockStatus(int)`: Query individual lock status
- `getAllLocksStatus()`: Query all locks status

### LockCtlBoardCmdHelper

- `buildOpenSingleLockCommand()`: Construct 0x82 instruction
- `buildOpenMultipleLocksCommand()`: Construct 0x80 instruction
- `buildFlashChannelCommand()`: Construct 0x81 instruction
- `buildChannelKeepOpenCommand()`: Construct 0x88 instruction
- `buildCloseChannelCommand()`: Construct 0x89 instruction
- `buildGetSingleLockStatusCommand()`: Construct 0x83 instruction
- `buildGetAllLocksStatusCommand()`: Construct 0x84 instruction
- `buildOpenAllLocksCommand()`: Construct 0x86 instruction
- `buildOpenMultipleSequentialCommand()`: Construct 0x87 instruction
- `parseResponseToJson()`: Parse binary responses to JSON format
- `validateResponse()`: Validate response frame integrity

## UI Features

- **Main Activity**: Comprehensive lock control dashboard with individual lock buttons, batch
  operations, and real-time response display
- **Sequential Open Activity**: Mode switching between sequential and simultaneous lock opening with
  multi-select capability
- **LED Flash Activity**: Individual LED control for each channel with direct execution
- **Auto-Connect**: Automatic serial port connection on application startup with status indicators
- **Response Display**: Real-time scrolling response data with timestamps and color-coded status
- **Tablet Optimization**: Efficient space utilization with 4-column button grid and integrated
  response area

## Testing

The project includes comprehensive unit tests for all lock control instructions:

- **Command Construction Tests**: Verify correct byte-level command construction for all
  instructions (0x80-0x84, 0x86-0x89)
- **Response Parsing Tests**: Test JSON response parsing for all supported response types
- **Checksum Validation Tests**: Verify XOR checksum calculation and validation
- **Edge Case Tests**: Test boundary conditions and error scenarios

## Error Handling

- **Serial Connection Management**: Proper error handling for connection failures and reconnection
  attempts
- **Data Validation**: Frame validation and checksum verification for incoming data
- **Fragmented Data Handling**: Buffer management for incomplete or fragmented serial data packets
- **UI Error Display**: User-friendly error messages and status indicators
- **Thread Safety**: Synchronized operations for concurrent access to shared resources

## Development Notes

- The application targets tablet devices with screen space optimization
- Serial communication uses a singleton pattern for resource management
- UI updates are handled through Android Handlers for main thread operations
- Response data is limited to recent entries to prevent memory overflow
- The system supports both sequential and simultaneous lock operations based on user preference
- Channel keep-open functionality supports timed operations for extended access control