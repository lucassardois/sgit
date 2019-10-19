package com.sardois.sgit

import org.scalatest._

class RepositoryIndexSpec extends FlatSpec {

    "A repository index" should "be able to add files" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))

        val index = repo/Repository.indexPath
        val lines = index.lines().toList
        assert(lines.length == files.length)

        val entry = lines(0)
        val split = entry.split(" ")
        if (split.length < 2) {
            fail("Invalid entry in the index")
        }

        val path = split(0)
        val sha = split(1)

        assert(path == Repository.relativize(repo, file))
        assert(sha == Util.shaFile(file))

        IORepositoryTest.delete(repo)
    }

    it should "be able to add nested files" in {
        val repo = IORepositoryTest.init()
        val nested = (repo.parent/"nested").createDirectories()
        val veryNested = (nested/"nestedAgain").createDirectories()
        IOTest.createRandomFile(veryNested)
        val files = Util.filesToPath(nested)

        IOIndex.add(repo, repo.parent, Config(paths = files))

        val indexFile = IOIndex.getIndexFile(repo)
        assert(indexFile.lines.size == files.length)

        IORepositoryTest.delete(repo)
    }

    it should "be able to add deleted file" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        file.delete()
        IOIndex.add(repo, repo.parent, Config(paths = files))

        val indexFile = IOIndex.getIndexFile(repo)
        assert(indexFile.lines.isEmpty)

        IORepositoryTest.delete(repo)
    }

    it should "be able to add files and deleted files" in {
        val repo = IORepositoryTest.init()
        val folder = (repo.parent/"folder").createDirectories()
        val file = IOTest.createRandomFile(folder)
        IOTest.createRandomFile(folder)
        val files = Util.filesToPath(folder)

        IOIndex.add(repo, repo.parent, Config(paths = files))

        val firstIndexFile = IOIndex.getIndexFile(repo)
        assert(firstIndexFile.lines.size == 2)

        file.delete()
        IOIndex.add(repo, repo.parent, Config(paths = files))

        val secondIndexFile = IOIndex.getIndexFile(repo)
        assert(secondIndexFile.lines.size == 1)

        IORepositoryTest.delete(repo)
    }

    it should "be able to add deleted files in nested folder" in {
        val repo = IORepositoryTest.init()
        val folder = (repo.parent/"folder").createDirectories()
        val file = IOTest.createRandomFile(folder)
        val files = Util.filesToPath(folder)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        file.delete()
        IOIndex.add(repo, repo.parent, Config(paths = files))

        val indexFile = IOIndex.getIndexFile(repo)
        assert(indexFile.lines.isEmpty)

        IORepositoryTest.delete(repo)
    }

    it should "write added files as blobs" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))

        IORepositoryTest.delete(repo)
    }

    it should "not write two identical files as two blobs" in {
        // But, it should still write two index entry pointing to only one blob
        val repo = IORepositoryTest.init()
        val originalFile = IOTest.createRandomFile(repo.parent)
        // Create a copy of the previous file (to get the same sha)
        val copiedFile = (repo.parent/"copy").write(originalFile.contentAsString)

        if (Util.shaFile(copiedFile) != Util.shaFile(originalFile)) {
            fail("The copied file have a different sha than the original")
        }

        val fileList = Util.filesToPath(originalFile, copiedFile)

        IOIndex.add(repo, repo.parent, Config(paths = fileList))

        // The number of index entry should be two
        val indexFile = IOIndex.getIndexFile(repo)
        assert(indexFile.lineCount == 2)

        // Only one blob should had been created since
        // the two added files have the same sha
        val blobFolder = IOBlob.getBlobsFolder(repo)
        assert(blobFolder.list.size == 1)

        IORepositoryTest.delete(repo)
    }

    it should "be readable" in {
        val repo = IORepositoryTest.init()
        val dir = repo.parent

        val filesPath = Util.filesToPath(
            IOTest.createRandomFile(dir),
            IOTest.createRandomFile(dir),
            IOTest.createRandomFile(dir)
        )
        IOIndex.add(repo, repo.parent, Config(paths = filesPath))

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)

        assert(indexFile.isRegularFile)
        assert(index.size == filesPath.length)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list untracked files" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = List(file.pathAsString)

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)
        val untrackedFiles = IOIndex.getUntrackedFiles(repo, index, files)

        assert(untrackedFiles.size == files.size)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list untracked nested files" in {
        val repo = IORepositoryTest.init()
        val nested = (repo.parent/"nested").createDirectories()
        val file1 = IOTest.createRandomFile(nested)
        val nestedAgain = (nested/"nestedAgain").createDirectories()
        val file2 = IOTest.createRandomFile(nestedAgain)

        val files = List(file1.pathAsString, file2.pathAsString)

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)
        val untrackedFiles = IOIndex.getUntrackedFiles(repo, index, files)

        assert(untrackedFiles.size == files.size)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list not staged modified files" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOTest.modifyRandomFile(file)

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)
        val modifiedFiles = IOIndex.getNotStagedModifiedFiles(repo, index, files)

        assert(modifiedFiles.size == 1)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list not staged deleted files" in {
        val repo = IORepositoryTest.init()
        val file1 = IOTest.createRandomFile(repo.parent)
        val file2 = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file1, file2 )

        IOIndex.add(repo, repo.parent, Config(paths = files))
        file1.delete()

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)
        val deletedFiles = IOIndex.getNotStagedDeletedFiles(repo, index, files)
        assert(deletedFiles.size == 1)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list staged new files" in {
        val repo = IORepositoryTest.init()
        val file1 = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file1)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(paths = files))

        val file2 = IOTest.createRandomFile(repo.parent)
        val newFiles = Util.filesToPath(file1, file2)
        IOIndex.add(repo, repo.parent, Config(paths = newFiles))

        val newIndex = IOIndex.getIndex(repo)
        val oldIndex = IOHead.getOldIndex(repo)
        val addedFiles = IOIndex.getStagedNewFiles(newIndex, oldIndex)

        assert(addedFiles.size == 1)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list staged modified files" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage =  "Test"))
        IOTest.modifyRandomFile(file)
        IOIndex.add(repo, repo.parent, Config(paths = files))

        val newIndex = IOIndex.getIndex(repo)
        val oldIndex = IOHead.getOldIndex(repo)
        val modifiedFiles = IOIndex.getStagedModifiedFiles(newIndex, oldIndex)

        assert(modifiedFiles.size == files.length)

        IORepositoryTest.delete(repo)
    }

    it should "be able to list staged deleted files" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage =  "Test"))
        file.delete()
        IOIndex.add(repo, repo.parent, Config(paths = files))

        val newIndex = IOIndex.getIndex(repo)
        val oldIndex = IOHead.getOldIndex(repo)
        val deletedFiles = IOIndex.getStagedDeletedFiles(newIndex, oldIndex)

        assert(deletedFiles.size == files.length)

        IORepositoryTest.delete(repo)
    }

    it should "return a string when asking for the status" in {
        val repo = IORepositoryTest.init()

        val str = IOIndex.status(repo, repo.parent, Config())

        assert(str.isDefined)

        IORepositoryTest.delete(repo)
    }

    it should "know if a repository has not staged changes" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage = "Test"))
        IOTest.modifyRandomFile(file)

        val newIndex = IOIndex.getIndex(repo)
        val oldIndex = IOHead.getOldIndex(repo)
        val hasChanges = IOIndex.haveUncommitedChanges(repo, newIndex, oldIndex, files)

        assert(hasChanges)

        IORepositoryTest.delete(repo)
    }
}