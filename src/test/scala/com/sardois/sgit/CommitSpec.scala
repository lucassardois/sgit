package com.sardois.sgit

import org.scalatest._

class CommitSpec extends FlatSpec {

    "A commit" should "be doable" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)
        val message = "Test commit"

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage = message))

        val indexFile = IOIndex.getIndexFile(repo)
        val index = IOIndex.read(indexFile)
        val indexSha = index.sha()
        val commit = Commit(message, indexSha, Commit.root.sha)
        val newCommitFile = repo/Repository.commitsPath/commit.sha

        // The format of the commit is correct
        assert(newCommitFile.contentAsString == commit.toString)

        // The index was saved
        val savedIndexedFile = IOIndex.getIndexesFolder(repo)/indexSha
        assert(savedIndexedFile.exists)
        assert(savedIndexedFile.contentAsString == index.toString)

        // The commit sha referenced by the HEAD was updated
        val newCommitSha = IOHead.getPointedCommitSha(repo)
        assert(newCommitSha == commit.sha)

        IORepositoryTest.delete(repo)
    }

    it should "be readable" in {
        val repo = IORepositoryTest.init()
        val file = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file)
        val message = "Test commit"

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage = message))

        val commitsFolder = IOCommit.getCommitsFolder(repo)
        val commitSha = IOHead.getPointedCommitSha(repo)
        val commit = IOCommit.read(commitsFolder, commitSha)

        assert(commit.message == message)
        assert(commit.sha == commitSha)

        IORepositoryTest.delete(repo)
    }

    it should "be checkable" in {
        val repo = IORepositoryTest.init()
        val file1 = IOTest.createRandomFile(repo.parent)
        val file2 = IOTest.createRandomFile(repo.parent)
        val files = Util.filesToPath(file1, file2)
        val messageCommit1 = "Commit 1"
        val messageCommit2 = "Commit 2"

        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage = messageCommit1))
        file1.delete()
        IOIndex.add(repo, repo.parent, Config(paths = files))
        IOCommit.commit(repo, repo.parent, Config(commitMessage = messageCommit2))

        val secondCommit = IOHead.getPointedCommit(repo)
        val commitsFolder = IOCommit.getCommitsFolder(repo)
        val firstCommit = IOCommit.read(commitsFolder, secondCommit.parentCommitSha)

        IOCommit.checkout(repo, secondCommit)
        assert(IOHead.getPointedCommit(repo).message == messageCommit2)
        assert(!file1.exists)
        assert(file2.exists)

        IOCommit.checkout(repo, firstCommit)
        assert(IOHead.getPointedCommit(repo).message == messageCommit1)
        assert(file1.exists)
        assert(file2.exists)

        IORepositoryTest.delete(repo)
    }
}