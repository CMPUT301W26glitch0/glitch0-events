# CMPUT301_APP - Test Data and Unit Testing

This project includes a dedicated system for managing realistic test data in Firestore and a set of unit tests for the core data models.

## 1. Test Data Management

The `TestDataManager` class provides a simple way to populate and clean up your Firestore database.

### Terminal Commands (Recommended)
You can run these commands from your terminal (make sure an emulator or physical device is connected):

| Action | Terminal Command |
| :--- | :--- |
| **Add Test Data** | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cmput301_app.TestDataRunner#addData` |
| **Delete Test Data** | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cmput301_app.TestDataRunner#deleteData` |

### Java Code Usage
If you want to trigger these from within your app's UI (e.g., a hidden "Dev Tools" button):
- **Add:** `new TestDataManager().addTestData();`
- **Delete:** `new TestDataManager().deleteAllTestData();`

---

## 2. Unit Testing

Core business logic is verified using JUnit 4 tests. These do **not** require an emulator.

### Location
`app/src/test/java/com/example/cmput301_app/model/ModelTest.java`

### Running the Tests
- **In Android Studio:** Right-click `ModelTest.java` and select **Run 'ModelTest'**.
- **In Terminal:** `./gradlew testDebugUnitTest`

---

