# Filepicker
Simple Android filepicker widget supporting multiple selection

This is a Filepicker module for Android. Supports
 - multiple selections (folders and files)
 - creation of new directories
 - use as a fragment or individual activity

This is based on Jonas Kalderstam's work (https://github.com/spacecowboy/NoNonsense-FilePicker)
but much simplified (less generic, supports only real files/directories, less than half the
codebase) with some improvements (survives rotations, supports SD-cards).

Lacks proper documentation unfortunately, but the 2 key classes FilePickerActivity/
FilePickerFragment are easy to understand and just roughly 50/150 lines of code.
