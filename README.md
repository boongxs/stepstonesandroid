# Stepstones Media Organizer for Android
Stepstones is a local, offline media organizer. A mobile companion to the [Stepstones desktop app](https://github.com/boongxs/StepstonesFlutter). It lets you bring your organized media 
collections with you, import new files on the go, and sync content between devices using `.stepstone` bundles.

<div align="center">
    <img width="350" height="693" alt="Screenshot_20260510_205931_StepstonesFlt" src="https://github.com/user-attachments/assets/608cbe1a-b4da-48d4-a57d-591a02a440d9"/>
</div>

## Features
- **Importing media** - To import media items, use the share sheet from any app (gallery, file manager, browser, etc.) and select Stepstones to import all of the selected media items
- **Albums** - Organize your media into albums. The default Library album is always present and cannot be deleted. A single file is stored on disk only once, adding it to multiple albums
creates a link rather than a copy, so no storage is wasted.
- **Media grid** - The main view shows your media in a grid. GIF files are labelled with a GIF badge. Video and audio files display their duration. Tap any item to open it in Enlarge view,
or long-press to enter selection mode.
- **Enlarge view** - Tap any item to open it fullscreen. Swipe left and right to navigate between items in the current album or active search results. Images and GIFs are able to be zoomed in.
  Tap anywhere on the media to show or hide the toolbar. The toolbar contains:
  - Info - Opens the info panel, allowing you to set a custom date and time for the file (e.g. when a photo was taken). You can also manage tags here to help find the right media file quicker.
  - Share - Allows you to share the file to another app
  - Add to album - Import the file to a different album
  - Delete - Remove the file from current album
- **Tags** - To easily find the correct media file for the situation, you can assign tags in the file's info panel. After that use the search bar to quickly filter between all imported media items,
finding the file in large collections
- **Selection mode** - Long-press any media item to enter selection mode. Tap additional items to add them to the selection, or select all by tapping the "All" button. With the selection you can:
  - Share - Shares all selected files to another app
  - Add to album - Imports all selected items to another album
  - Export - Exports selected items as a `.stepstone` bundle
  - Delete - Removes selected items from current album
- **Bundles** - Stepstones can export and import `.stepstone` files, a portable bundle format containing media files, their thumbnails, and metadata (tags, file type, dimensions, duration). You can import
the bundle in any version of Stepstones, desktop or mobile.
- **Duplicate review** - When a new image is imported, Stepstones computes a perceptual hash and compares it against existing images in the same album. If a near-identical image is found, the pair is queued for
review in the Review tab. The Review tab shows both images side by side along with a similarity percentage. You can keep both or discard the imported one. Swipe to Review tab from the main grid to access pending
reviews - a dot indicator appears on the tab when reviews are waiting.
