//
//  EditSeriesSheet.swift
//  iosApp
//
//  Created by Owen LeJeune on 2026-02-03.
//

import SwiftUI
import Shared

struct EditSeriesSheet: View {
    let item: ArrSeries
    let qualityProfiles: [QualityProfile]
    let rootFolders: [RootFolder]
    let tags: [Tag]
    let editInProgress: Bool
    let onEditItem: (ArrSeries, Bool) -> Void
    
    @Environment(\.dismiss) var dismiss
    
    @State private var monitored: Bool
    @State private var monitorNewSeasons: Bool
    @State private var qualityProfileId: Int32
    @State private var seriesType: SeriesType
    @State private var seasonFolders: Bool
    @State private var rootFolder: String
    @State private var selectedTags: Set<Int>
    
    @State private var moveFiles: Bool = false
    
    private var canMove: Bool {
        rootFolder != item.rootFolderPath
    }
    
    init(item: ArrSeries, qualityProfiles: [QualityProfile], rootFolders: [RootFolder], tags: [Tag], editInProgress: Bool, onEditItem: @escaping (ArrSeries, Bool) -> Void) {
        self.item = item
        self.qualityProfiles = qualityProfiles
        self.rootFolders = rootFolders
        self.tags = tags
        self.editInProgress = editInProgress
        self.onEditItem = onEditItem
        
        self.monitored = item.monitored
        self.monitorNewSeasons = item.monitorNewItems == .all
        self.qualityProfileId = item.qualityProfileId
        self.seriesType = item.seriesType
        self.seasonFolders = item.seasonFolder
        self.rootFolder = item.rootFolderPath ?? ""
        self.selectedTags = Set(item.tags.map(\.intValue))
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(MR.strings().monitored.localized(), isOn: $monitored)
                    Toggle(MR.strings().monitor_new_seasons.localized(), isOn: $monitorNewSeasons)
                    Toggle(MR.strings().season_folders.localized(), isOn: $seasonFolders)
                    Picker(MR.strings().series_type.localized(), selection: $seriesType) {
                        ForEach(SeriesType.allCases, id: \.self) { type in
                            Text(type.resource.localized()).tag(type)
                        }
                    }
                    Picker(MR.strings().quality_profile.localized(), selection: $qualityProfileId) {
                        ForEach(qualityProfiles, id: \.id) { qp in
                            Text(qp.name ?? "").tag(qp.id)
                        }
                    }
                    if tags.count > 0 {
                        NavigationLink {
                            TagSelectionView(tags: tags, selectedTags: $selectedTags)
                        } label: {
                            LabeledContent(
                                MR.strings().tags.localized(),
                                value: MR.plurals().tag_count.localized(selectedTags.count)
                            )
                        }
                    }
                }
                
                Section {
                    if rootFolders.count > 1 {
                        Picker(MR.strings().root_folder.localized(), selection: $rootFolder) {
                            ForEach(rootFolders, id: \.id) { folder in
                                Text("\(folder.path) (\(folder.freeSpace.bytesAsFileSizeString()))")
                                    .tag(folder.path)
                            }
                        }
                        if canMove {
                            Toggle(MR.strings().move_files.localized(), isOn: $moveFiles)
                        }
                    }
                } footer: {
                    if canMove {
                        Text(MR.strings().move_files_description.localized())
                    }
                }
            }
            .toolbarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    Button {
                        dismiss()
                    } label: {
                        Label(MR.strings().close.localized(), systemImage: "xmark")
                            .foregroundStyle(.white)
                    }
                    .tint(nil)
                }
                
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        let newSeries = item.doCopyForEdit(
                            monitored: monitored,
                            monitorNewSeasons: monitorNewSeasons ? .all : .none,
                            qualityProfileId: qualityProfileId,
                            seriesType: seriesType,
                            seasonFolder: seasonFolders,
                            rootFolderPath: rootFolder,
                            tags: Array(selectedTags.map { $0.asKotlinInt })
                        )
                        onEditItem(newSeries, moveFiles && canMove)
                    } label: {
                        if editInProgress {
                            ProgressView()
                                .progressViewStyle(.circular)
                                .foregroundStyle(.white)
                        } else {
                            Label(MR.strings().save.localized(), systemImage: "checkmark")
                                .foregroundStyle(.white)
                        }
                    }
                    .tint(nil)
                }
            }
        }
    }
}
