package com.sardois.sgit

import better.files.File

object Command {

    @impure
    def add(repository: Repository, config: Config): Either[String, String] = {
        val files = config.paths
            .map(path => File(path))
            .toList

        val filesToRemove = files.map(file => repository.relativize(file))

        val filesToAdd = files
            .filter(file => file.exists)
            .map(file => file :: file.listRecursively.toList).flatten
            .filter(file => {
                !(
                    file.isDirectory ||
                    file == repository.repositoryFolder ||
                    file.isChildOf(repository.repositoryFolder)
                )
            })
            .map(file => (repository.relativize(file), Util.shaFile(file)))

        val blobs = filesToAdd.map( (tuple) => {
            val file = File(tuple._1)
            val sha = tuple._2
            Blob(repository, sha, file.contentAsString)
        })

        for {
            currentIndex <- repository.notCommitedCurrentIndex
            indexAfterRemove <- Right(currentIndex.removeAll(filesToRemove))
            indexAfterAdd <- Right(indexAfterRemove.addAll(filesToAdd))
            _ <- IO.write(indexAfterAdd)
            result <- IO.writeAll(blobs)
        } yield result
    }

    @impure
    def commit(repository: Repository, config: Config): Either[String, String] = {
        val commitMessage = config.commitMessage

        for {
            currentIndex <- repository.notCommitedCurrentIndex
            lastCommitSha <- repository.lastCommitSha
            newCommit <- Right(Commit(repository, commitMessage, currentIndex.sha, lastCommitSha))
            head <- repository.head
            branch <- head.branch
            newBranch <- Right(branch.moveToCommit(newCommit.sha))
            _ <- IO.write(currentIndex.toStagedIndex())
            _ <- IO.write(newCommit)
            result <- IO.write(newBranch)
        } yield result
    }

    @impure
    def status(repository: Repository, config: Config): Either[String, String] = {
        val modifiedIndex = repository.listPotentiallyModifiedFilesAsIndex()
        val deletedIndex = repository.listPotentiallyDeletedFilesAsIndex()
        val untrackedFiles = repository.listPotentiallyUntrackedFiles()

        val indexes = for {
            head <- repository.head
            branch <- head.branch
            notCommitedCurrentIndex <- repository.notCommitedCurrentIndex
            lastCommitedIndex <- repository.lastCommitedIndex
        } yield (branch, notCommitedCurrentIndex, lastCommitedIndex)

        indexes.map( tuple => {
            val branch = tuple._1
            val uncommitedCurrentIndex = tuple._2
            val lastCommitedIndex = tuple._3

            val finalStr = UI.status(
                branch.name,
                uncommitedCurrentIndex.newfiles(lastCommitedIndex),
                uncommitedCurrentIndex.modified(lastCommitedIndex),
                lastCommitedIndex.deleted(uncommitedCurrentIndex),
                uncommitedCurrentIndex.modified(modifiedIndex),
                uncommitedCurrentIndex.deleted(deletedIndex),
                uncommitedCurrentIndex.untracked(untrackedFiles)
            )

            Right(finalStr)
        }).flatten
    }

    @impure
    def createBranch(repository: Repository, config: Config): Either[String, String] = {
        val branchName = config.branchName

        val tuple = for {
            lastCommitSha <- repository.lastCommitSha
            branches <- repository.branches
        } yield (lastCommitSha, branches)

        tuple.map( t => {
            val lastCommitSha = t._1
            val branches = t._2

            val branchExists = branches.exists( branch => branch.name == branchName)
            if (branchExists) {
                return Left("A branch with this name already exists.")
            }

            IO.write(Branch(repository, branchName, lastCommitSha))
        }).flatten
    }

    @impure
    def createTag(repository: Repository, config: Config): Either[String, String] = {
        val tagName = config.tagName

        val tuple = for {
            lastCommitSha <- repository.lastCommitSha
            tags <- repository.tags
        } yield (lastCommitSha, tags)

        tuple.map( t => {
            val lastCommitSha = t._1
            val tags = t._2

            val tagExists = tags.exists( tag => tag.name == tagName)
            if (tagExists) {
                return Left("A tag with this name already exists.")
            }

            IO.write(Tag(repository, tagName, lastCommitSha))
        }).flatten
    }

    @impure
    def listBranchAndTags(repository: Repository, config: Config): Either[String, String] = {
        val data = for {
            currentBranch <- repository.branch
            branches <- repository.branches
            tags <- repository.tags
        } yield (currentBranch, branches, tags)

        data.map(d => {
            val currentBranch = d._1
            val branches = d._2
            val tags = d._3

            Right(UI.branchAndTags(currentBranch, branches, tags))
        }).flatten
    }

    @impure
    def checkout(repository: Repository, config: Config): Either[String, String] = {
        repository.hasUncommitedChanges.map( result => {
            if (result) {
                return Left("You have uncommited changes, please first commit before checkout.")
            }
        })

        // TODO
        Right("Checkout ready")
    }

    @impure
    def log(repository: Repository, config: Config): Either[String, String] = {
        repository.commits.map( commits => {
            val sortedCommits = commits.sortWith( (c1, c2) => {
                c1.date.after(c2.date)
            })
            UI.log(sortedCommits)
        })
    }
}

