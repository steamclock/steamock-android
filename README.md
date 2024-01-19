# Postman Mocking Library

One of our current initiatives at Steamclock is to bring our mocked data into alignment between platforms on projects.
Our main goals are to:
* Have a single source of truth for our mock data.
* Have a unified UI for displaying, enabling and disabling mocks in our apps.
* Improve our ability to easily add and modify existing mocked data (ideally without having to rebuild projects).

One area of research has been to use Postman's mocking server framework to house our mocks. This library serves as an example on how we use Postman's APIs to get access to mocks and to supply a common UI for listing, enabling and disabling desired mocks on Android applications.

*Disclaimer: This repo is still under active development, and will most likely change and evolve over time.*

## What is contained in this repo?
This repo is split into a few modules:
1. **app**: A quick and dirty example on how to use the underlying library.
2. **lib-core**: The core logic for our library, it contains the code that queries Postman, the Repository for tracking the list of all available mocks and which of these have been enabled, as well as some Composable functions which create the UI to interact with all available mocks.
3. **lib-ktor**: Contains the interceptor for hooking into apps that use the Ktor networking library.
4. **lib-retrofit**: Contains the interceptor for hooking into apps that use the Retrofit networking library.


## Running the sample app
The sample app will not run on it's own, and requires some information about the Postman mocking environment - all of which will be placed in the project's `local.properties`.

1. Clone the repo!
2. Open the `local.properties` file and add the following properties:
```
postmanAccessKey = <Your Postman API access key> 
postmanCollectionId = <The Postman mocked collection ID>  
postmanMockServerUrl = <The Postman Mock Server URL>

# The following only needed if running the sample app. 
# If adding the library to your project directly, you will not need this. 
exampleDefaultUrl = <Default URL for the sample app>
```
So for example:
```
postmanAccessKey = "PMAKXXXXXXXXXXXX-XXXXXXXXXXXXXXd0e2d"  
postmanCollectionId = "8183416-XXXXXXXX-XXXX-XXXX-XXXX-f6b920992f1d"  
postmanMockServerUrl = "https://1c6a81de-XXXX-XXXX-XXXX-XXXXX0471766.mock.pstmn.io"  
exampleDefaultUrl = "https://myWebsite.com/api/"
```

3. Build the project! The sample app will not build if any of the above are missing.

*Note: For our Steamclock projects, we will put information regarding our mock environments in relevant Coda documents*

### Where to find Postman keys, etc...
#### Postman Access Key
This key is a personal access key for the user account that will be querying the Postman mock collections. This user must have proper permissions to the mocked collections. Ideally this API key will be setup on a shared account, however, you can setup your own access key by following the instructions at: https://learning.postman.com/docs/developer/postman-api/authentication/

#### Postman Collection ID
This is the ID for the collection that contains all of the saved mocks. It can be found in Postman by clicking on the desired Collection and then clicking on the *Info* icon on the far right column. The ID should be listed at the top of the Collection Details.

#### Postman Mock Server URL
This is the URL that can be used to access the saved mocked responses in a Collection. Once a mocking server has been enabled for a Collection (todo: readme for setting up Mock server), click on the Mock Servers menu on the left, select the desired mock  server, and then click the *info* icon on the far right column. The mock server URL should be listed part way down the details panel.    *Note: For some reason this URL seems to change on occasion. As such, there is a good chance this property will need to updated more frequently than the others*

#### Example Default URL
This URL will be very tightly tied to the data you are mocking, and is mostly a convenience for the sample app to avoid you having to enter in the full URL path to test out each time the app is run. When the library is used in a separate project, you should not need to set this, as your actual app should contain repositories that correctly run your APIs already.


## Using the library

todo: Expand on how to setup; for now reference the sample app
Should be able to make public and put on Jitpack soon

### Important Components

#### 1. `PostmanMockConfig`
Contains the configuration for our mocking server.

#### 2. `PostmanMockRepo`
This contains the list of all available mocks and which have been enabled; if your application is using the `AvailableMocks` Composable.

#### 3. `PostmanMockInterceptorKtor` / `PostmanMockInterceptorRetrofit`
These interceptors are responsible for intercepting all requests, and redirecting to use desired mocks when applicable.

#### 4. `AvailableMocks` Composable
When paired with a `PostmanMockRepo`, this Composable will list all available mocks and allow users to easily enable and disable desired mocks. It’s purpose is to make it as easy as possible to use mocks, and to make the mocking experience as unified as possible across Android projects.
 