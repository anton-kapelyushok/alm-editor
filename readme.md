## ALM Concurrent Editor

The following test task should take 4-6 hours to complete. If you have spent 8 hours or more on this task, please send us whatever results you have.

Some problems to be solved in this task are close to the problem that we’re dealing with in our commercial products.

When assessing the result of your work, we will be paying most attention to the architecture and design of the application, performance, code quality and usability. 

(A note on comments: you don’t need to comment everything! Leave only comments that provide considerable help in reading the code. Comments must be in English.)

Along with the resulting source code, please submit a short description of the architecture (2-3 paragraphs), in English. 


Concurrent Editing Model
------------------------

You're in a team that builds an online concurrent editing tool, something like Google Docs. When the tool is ready, multiple users will be able to log in and work on the shared documents at the same time. 

You are responsible for the server-side model of this system. You will need  to build a model that would support at least the following features of the app:

* Live updates - when one user enters text, the other users see it, maybe with a little delay but without reloading the page.

* Concurrent editing - multiple users can make edits at the same time. Concurrent edits should always merge and never result in a conflict.


You're encouraged to think about other properties that such system must have.

The test task is: think about the architecture of this application and create a model in Java for it. The model should include: 
a) data structures to keep data in memory; 
b) interfaces for working with this data that would be used when implementing features listed above; 
c) implementation of these interfaces.

Assume that everything happens in memory, don't bother with databases or other storage.

The format is plain text (no formatting!) and the language is known to be English.

You can use:

* JDK 1.8
* Kotlin
* Web technology stack of your choice (Spring / Akka HTTP / Play / Ktor / vert.x)
* Apache Commons (any library)
* Guava

------

Optional tasks:

1. Provide RESTful API for the model (specify the protocol without the implementation)

2. Implement the RESTful API

3. Undo / redo for each individual user.



#### Build and execute
`gradlew clean build bootRun`

#### API Description

Service provides 4 methods for operating on documents:

- `createDocument` - accepts document id, creates document with this id in repository
- `applyPatch` - accepts document id and patch (see below), applies patch to the document
- `getDocumentContent` - returns current document state and it's revision number
- `getDocumentDiff` - accepts document id and revision, returns difference between current and client revisions


Patch is and object containing its id (should be unique across document) and changes client wants to apply to the document

3 types of changes are supported:

- `AddLineUpdate(lineIndex)` - adds line at index
- `RemoveLineUpdate(lineIndex)` - removes line at index
- `ChangeLineUpdate(lineIndex, update)` - changes line at index
   - `update` can be `AddSymbols(index, symbols)` or `RemoveSymbols(fromIndex, toIndex)` 
   
Before saving client changes are merged with existing changes client did not know about.
Strategy is the following:

 1. We find missing updates using passed revision 
 1. Apply server `add` and `remove` line updates to client patches 
 1. Then we move server and client `change` updates to the same index space:
    - Apply server `add` and `remove` updates that were after server `change` update to it
    - Apply client `add` and `remove` updates to server `change` updates
    - Apply client `add` and `remove` updates that were after client `change` update to it
 1. As they are in the same index space now we can merge them 
 (refer to `PatchService:mergeLineUpdate` and `MergeLineUpdatesTest`)
 
Client logic would be the following
1. Collect updates locally
1. Send them to the server
1. After receiving diff update remove updates that were acknowledged by the server, 
update local document state and not sent updates using same logic as implemented on server
1. repeat
