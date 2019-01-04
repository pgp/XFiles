package it.pgp.xfiles.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/** Select a directory or a file. 
 * 
 * */
public class FileSelectFragment extends DialogFragment 
		implements OnItemClickListener, OnItemLongClickListener {

	/*
	 * Use the unicode "back" triangle to indicate there is a parent 
	 * directory rather than an icon to minimise file dependencies.
	 * 
	 * You may have to find an alternative symbol if the font in use 
	 * doesn't support this character. 
	 * */ 
	final String PARENT =  "\u25C0"; 
	
	private Callbacks mCallbacks;
	private ArrayList<File> fileList;
	
	// The widgets required to provide the UI.
	private TextView selectedPath;
	private TextView selectedFile;
	private LinearLayout root;
	private ListView directoryView;

	// The directory the user has selected.
	private File currentDirectory;
	private File currentFile;
	
	// Resource IDs
	private int resourceID_OK;
	private int resourceID_Cancel;
	private int resourceID_Title;
	private int resourceID_Icon; 
	private int resourceID_Dir;
	private int resourceID_File;

	// How the popup is to be used.
	private Mode selectionMode;
	
	// Filtered view of the directories.
	private FilenameFilter fileFilter;
	
	/** How do we want to use the selector? */
	public enum Mode {
		DirectorySelector,
		FileSelector
	}
	
	/** 
	 * Signal to / request action of host activity.
	 * 
	 * */
	public interface Callbacks {

		/**  
		 * Hand selected path and name to context for use.
		 * If user cancels absolutePath and filename are handed out as null.
		 * 
		 *  @param absolutePath - Absolute path to target directory.
		 *  @param fileName     - Filename. Will be null if Mode = DirectorySelector
		 *    
		 * */
		void onConfirmSelect(String absolutePath, String fileName);
		
		
		/** Allow the client activity to check file content / format whilst the user
		 *  still has the popup in view.
		 *  
		 *  The alternative is to provide a custom filter that examines file content
		 *  as it goes, but that could get very slow very quickly especially for binary
		 *  files. 
		 *  
		 * */
		boolean isValid(String absolutePath, String fileName);
	
	}

	/** Provide a standard filter to match any file with an extension in the supplied list. 
	 *  Case insensitive..
	 *  Directories are always accepted.
	 *  
	 *   @param fileExtensions List of file extensions including full stop. .xml, .txt etc. 
	 * */
	public static FilenameFilter FiletypeFilter(final ArrayList<String> fileExtensions) {
		  return (directory, fileName) -> {

              File f = new File(String.format("%s/%s", directory.getAbsolutePath(), fileName));

              // We let all directories through.
              boolean matched = f.isDirectory();

              if (!matched) {
                  for(String s : fileExtensions) {
                      s = String.format(".{0,}\\%s$", s);
                      s = s.toUpperCase(Locale.getDefault());
                      fileName = fileName.toUpperCase(Locale.getDefault());
                      matched = fileName.matches(s);
                      if (matched) {
                          break;
                      }
                  }
              }

             return matched;

          };
	}
	
	/** Create new instance of a file save popup. 
	 * 
	 * @param selectionMode - Directory selector or File selector?
	 * @param resourceID_OK - String resource ID for the positive (OK) button.
	 * @param resourceID_Cancel - String resource ID for the negative (Cancel) button.
	 * @param resourceID_Title - String resource ID for the dialogue's title.
	 * @param resourceID_Icon - Drawable resource ID for the dialogue's title bar icon.
	 * @param resourceID_Directory - Drawable resource ID for a directory icon. Distinguish dirs from files.
	 * @param resourceID_File - Drawable resource ID for a file icon.
	 * */
    public static FileSelectFragment newInstance(Mode selectionMode,
    		                                     int resourceID_OK,
    		                                     int resourceID_Cancel,
    		                                     int resourceID_Title,
    		                                     int resourceID_Icon,
    		                                     int resourceID_Directory,
    		                                     int resourceID_File,
												 String... startDirectory) {

    	FileSelectFragment frag = new FileSelectFragment();
    	
    	Bundle args = new Bundle();
    	args.putInt("mode", selectionMode.ordinal());
    	args.putInt("captionOK", resourceID_OK);
    	args.putInt("captionCancel", resourceID_Cancel);
    	args.putInt("popupTitle", resourceID_Title);
    	args.putInt("iconPopup", resourceID_Icon);
    	args.putInt("iconDirectory", resourceID_Directory);
    	args.putInt("iconFile", resourceID_File);
    	if (startDirectory.length > 0 && selectionMode == Mode.DirectorySelector)
    		args.putString("currentDirectory",startDirectory[0]);

    	frag.setArguments(args);
        return frag;
    }	
	
    /** Optional. Allow restriction of file names/types displayed for selection. 
     * @param fileFilter - May be null. Custom rule for selecting directories/files.
     * 
     * */
    public void setFilter(FilenameFilter fileFilter) {
    	this.fileFilter = fileFilter;
    }

    /** Note the parent activity for callback purposes.
     *  @param activity - parent activity
     * */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        // The containing activity is expected to implement the fragment's 
        // callbacks otherwise it can't react to item changes.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        fileList = new ArrayList<>();
        resourceID_OK = getArguments().getInt("captionOK");
        resourceID_Cancel = getArguments().getInt("captionCancel");
        resourceID_Title = getArguments().getInt("popupTitle");
        resourceID_Icon = getArguments().getInt("iconPopup");
        resourceID_File = getArguments().getInt("iconFile");
        resourceID_Dir = getArguments().getInt("iconDirectory");
        selectionMode = Mode.values()[getArguments().getInt("mode")];

        // current directory provided by user, or default if not provided
		String startDir = getArguments().getString("currentDirectory");
		if (startDir != null)
			currentDirectory = new File(startDir);
	}

	/** Build the popup */
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        /* 
         * Use the same callback for [OK] & [Cancel]. 
         * Hand out nulls to indicate abandonment. 
         * */
       
		/* 
		 * We want to make this a transportable piece of code so don't want an XML
		 * layout dependency so layout is set up in code.
		 * 
		 *   [ListView of directory & file names                         ]
		 *   [                                                    ]
		 *   [                                                    ]
		 *   [                                                    ]
		 *   ------------------------------------------------------
		 *   {current path}/ {selected file}  
		 * 
		 * */
	
		// Set up the container view.
		LinearLayout.LayoutParams rootLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				                                                             ViewGroup.LayoutParams.WRAP_CONTENT, 
				                                                             0.0F);
		root = new LinearLayout(getActivity());
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(rootLayout);

		/* 
		 * Set up initial sub-directory list.
		 * 
		 * */
		if (currentDirectory == null)
			currentDirectory = Environment.getExternalStorageDirectory();
		fileList = getDirectoryContent(currentDirectory);
		DirectoryDisplay displayFormat = new DirectoryDisplay(getActivity(), fileList);

		/*
		 * Fix the height of the listview at 150px, enough to show 3 or 4 entries at a time.
		 * Don't want the popup shrinking and growing all the time. Tried it. 
		 * Most disconcerting.
		 * */
		LinearLayout.LayoutParams listViewLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
																				ViewGroup.LayoutParams.MATCH_PARENT,
																				0.7F);
		directoryView = new ListView(getActivity());
		directoryView.setLayoutParams(listViewLayout);
		directoryView.setAdapter(displayFormat);
		/* 
		 * Click on file or directory - select
		 * Long click on directory    - open directory
		 *  */
		directoryView.setOnItemClickListener(this);
		directoryView.setOnItemLongClickListener(this);
		root.addView(directoryView);
		
		View horizDivider = new View(getActivity()); 
		horizDivider.setBackgroundColor(Color.CYAN);
		root.addView(horizDivider,
				     new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, 2));		

		/*
		 * Now set up the filename display area.
		 * 
		 * {current path}/ {selected file}
		 * 
		 * */
		LinearLayout nameArea = new LinearLayout(getActivity());
		nameArea.setOrientation(LinearLayout.HORIZONTAL);
		nameArea.setLayoutParams(rootLayout);
		root.addView(nameArea);
		
	    selectedPath = new TextView(getActivity());
		selectedPath.setText(currentDirectory.getAbsolutePath() + "/");
		nameArea.addView(selectedPath);
		
		// We only display a selected filename in FileSelector mode. 
		if (selectionMode == Mode.FileSelector ) {
			selectedFile = new TextView(getActivity());
			selectedFile.setGravity(Gravity.LEFT);
			selectedFile.setPadding(2, 0, 6, 0);
			nameArea.addView(selectedFile);
		}
		
		// Use the standard AlertDialog builder to create the popup. 
		// Avoid call chaining, keep the code readable and maintainable.
		Builder popupBuilder = new AlertDialog.Builder(getActivity());
		popupBuilder.setView(root);
		popupBuilder.setIcon(resourceID_Icon);
		popupBuilder.setTitle(resourceID_Title);

		// Set up anonymous methods to handle [OK] & [Cancel] clicks.
		popupBuilder.setPositiveButton(resourceID_OK,
				(dialog, whichButton) -> {/* Empty method. See onStart */});

		popupBuilder.setNegativeButton(resourceID_Cancel,
				(dialog, whichButton) -> mCallbacks.onConfirmSelect(null, null));
		
        return popupBuilder.create();
    }
	
	/** 
	 * Provide the [PositiveButton] with a click listener that doesn't 
	 * dismiss the popup if the user has chosen a file (or directory)
	 * that is unsuitable. 
	 * 
	 * */
	@Override
	public void onStart()
	{
	    super.onStart();    
	    AlertDialog d = (AlertDialog)getDialog();
	    if(d != null)
	    {
	        Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
	        positiveButton.setOnClickListener(v -> {

                String absolutePath = currentDirectory.getAbsolutePath();
                String filename = null;
                if(currentFile != null) {
                    filename = currentFile.getName();
                }

                if (mCallbacks.isValid(absolutePath, filename)) {
                    dismiss();
                    mCallbacks.onConfirmSelect(absolutePath, filename);
                }
            });
	    }
	}	

	/** Single short click/press selects a file.
	 * */
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {

		currentFile = null;
		
		String path = currentDirectory.getAbsolutePath();
		if (currentDirectory.getParent() != null) {
			path += "/";
		}
		selectedPath.setText(path);	

		if (pos >= 0 || pos < fileList.size()) {
			currentFile = fileList.get(pos);
			
			String name = currentFile.getName();
			
			if (!currentFile.isDirectory() && 
			    !name.equals(PARENT) &&
			    selectionMode == Mode.FileSelector ) {
				selectedFile.setText(currentFile.getName());
			}
				
			
		}
		
	}

	/** Long click/press on a directory selects and opens that directory. 
	 * */
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
		
		File selected;
		
		if (pos >= 0 || pos < fileList.size()) {
			selected = fileList.get(pos);
			String name = selected.getName();

			if (selected.isDirectory() || name.equals(PARENT)) {

				// Are we going up or down?
				if (name.equals(PARENT)) {
					currentDirectory = currentDirectory.getParentFile();
				}
				else {
					currentDirectory = 	selected;
				}

				// Refresh the listview display for the newly selected directory.
				fileList = getDirectoryContent(currentDirectory);
				DirectoryDisplay displayFormatter = new DirectoryDisplay(getActivity(), fileList);
				directoryView.setAdapter(displayFormatter);
				
				// Update the path TextView widgets.  Tell the user where he or she is and clear the selected file.
				currentFile = null;
				String path = currentDirectory.getAbsolutePath();
				if (currentDirectory.getParent() != null) {
					path += "/";
				}

				selectedPath.setText(path);	
				if (selectionMode == Mode.FileSelector) {
					selectedFile.setText(null);
				}
				
			}
			

		}

		
		return false;
	}

	/** Identify all sub-directories and files within a directory. 
	 *  @param directory The directory to walk.
	 * */
	private ArrayList<File> getDirectoryContent(File directory) {
		
		ArrayList<File> displayedContent = new ArrayList<>();

		// legacy
//		File[] files;
//		if (fileFilter != null) {
//	        files = directory.listFiles(fileFilter);
//		}
//		else {
//			files = directory.listFiles();
//		}

		// new
		GenericDirWithContent gdwc = MainActivity.getRootHelperClient().listDirectory(new LocalPathContent(directory.getAbsolutePath()));
		if (gdwc.errorCode != null) {
			Toast.makeText(MainActivity.mainActivityContext, "Error listing directories: "+gdwc.errorCode.getValue(), Toast.LENGTH_SHORT).show();
			return displayedContent;
		}
			
		// Allow navigation back up the tree when the directory is a sub-directory.
		if (directory.getParent() != null) {
			displayedContent.add(new File(PARENT));
		}
		
		// Get the content in this directory.
//		if (files != null) {
//			for (File f : files) {
//
//				boolean canDisplay = true;
//
//				if (selectionMode == Mode.DirectorySelector && !f.isDirectory()) {
//					canDisplay = false;
//				}
//
//				canDisplay = (canDisplay && !f.isHidden());
//
//				if (canDisplay) {
//					displayedContent.add(f);
//				}
//			}
//		}

		for (BrowserItem b : gdwc.content) {
			boolean canDisplay = true;

			if (selectionMode == Mode.DirectorySelector && !b.isDirectory)
				canDisplay = false;

			if (canDisplay) {
				displayedContent.add(new File(gdwc.dir+"/"+b.getFilename()));
			}
		}
		
		return displayedContent;
		
	}
	
	/** Display the sub-directories in a selected directory. 
	 * 
	 * */
	private class DirectoryDisplay 
		extends ArrayAdapter<File> {
		
		public DirectoryDisplay(Context context, List<File> displayContent) {
			super(context, android.R.layout.simple_list_item_1, displayContent);
		}
		
		/** Display the name of each sub-directory. 
		 * */
		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {

			int iconID = resourceID_File;
			// We assume that we've got a parent directory...
			TextView textview = (TextView) super.getView( position, convertView, parent );
			
			// If we've got a directory then get its name.
			// If it's a file we show the file icon, if a directory then the directory icon.
			if ( fileList.get(position) != null ) {
				String name = fileList.get(position).getName();
				textview.setText(name);

				if (fileList.get(position).isDirectory()) {
					iconID = resourceID_Dir;
				}

				if (name.equals(PARENT)) {
					iconID = -1;
				}
				
				// Icon to the left of the text.
				if (iconID > 0 ){
					Drawable icon = getActivity().getResources().getDrawable( iconID );
					textview.setCompoundDrawablesWithIntrinsicBounds(icon,null, null, null );	
				}
				
			}

			return textview;
		}		
		
		
	}

	
}
