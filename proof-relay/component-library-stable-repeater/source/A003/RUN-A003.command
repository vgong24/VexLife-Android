#!/bin/bash
set -euo pipefail

CONTINUITY='[VXG RealForever]'
SDK_REPOSITORY='vgong24/Vextreme-SDK'
SDK_HEAD='6f8f6f50467f5df5a542713f7dc25b82db4ac8fa'
SDK_TREE='96795640b01f1969da980f52d5a8ec136a8c44aa'
PRODUCT_REPOSITORY='vgong24/VexLife-Android'
PRODUCT_BASE='d857a92371555472e0303b18eadf353da2a868d6'
PRODUCT_HEAD='04a24d6d08c0afe5d6263db4652eb1e409162fe4'
PRODUCT_BRANCH='VXG-112126-component-stable-repeater-proof-a001'
PRODUCT_BASE_BRANCH='main'
ATTEMPT='A003'

resolve_exec() {
  local candidate
  for candidate in "$@"; do
    if [ -x "$candidate" ] && [ -f "$candidate" ]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

BASH_BIN="$(resolve_exec /bin/bash)" || { echo 'A003: qualified Bash unavailable.' >&2; exit 10; }
GIT_BIN="$(resolve_exec /usr/bin/git)" || { echo 'A003: qualified Git unavailable.' >&2; exit 11; }
GH_BIN="$(resolve_exec /opt/homebrew/bin/gh /usr/local/bin/gh)" || { echo 'A003: qualified GitHub CLI unavailable.' >&2; exit 12; }
NODE_BIN="$(resolve_exec /opt/homebrew/bin/node /usr/local/bin/node /usr/bin/node)" || { echo 'A003: qualified Node unavailable.' >&2; exit 13; }
MKTEMP_BIN="$(resolve_exec /usr/bin/mktemp)" || { echo 'A003: mktemp unavailable.' >&2; exit 14; }
RM_BIN="$(resolve_exec /bin/rm /usr/bin/rm)" || { echo 'A003: rm unavailable.' >&2; exit 15; }

ARCH="$(/usr/bin/uname -m)"
[ "$ARCH" = 'arm64' ] || { echo "A003: expected Apple-silicon arm64 host, observed $ARCH." >&2; exit 16; }

"$GH_BIN" auth status --hostname github.com >/dev/null 2>&1 || {
  echo 'A003: GitHub authentication is required before this proof can run.' >&2
  exit 17
}

ROOT="$("$MKTEMP_BIN" -d "$HOME/Downloads/.vexlife-android-pr261-stable-repeater-source-a003.XXXXXX")"
SDK_ROOT="$ROOT/Vextreme-SDK"
FORMATION_RESULT="$ROOT/formation-result.json"
FORMATION_SCRIPT="$ROOT/form-vexlife-android-pr261-stable-repeater-a003.mjs"

cleanup() {
  case "$ROOT" in
    "$HOME"/Downloads/.vexlife-android-pr261-stable-repeater-source-a003.*) "$RM_BIN" -rf -- "$ROOT" ;;
    *) return 1 ;;
  esac
}
trap cleanup EXIT HUP INT TERM

echo 'A003: deriving exact immutable Product tree from pinned commit...'
PRODUCT_TREE="$("$GH_BIN" api "repos/$PRODUCT_REPOSITORY/git/commits/$PRODUCT_HEAD" --jq '.tree.sha')"
"$NODE_BIN" -e 'if(!/^[0-9a-f]{40}$/.test(process.argv[1])) process.exit(1)' "$PRODUCT_TREE" || {
  echo "A003: invalid Product tree returned for exact head: $PRODUCT_TREE" >&2
  exit 18
}

echo 'A003: acquiring exact current VexRelay/VexLocalBridge source...'
"$GH_BIN" repo clone "$SDK_REPOSITORY" "$SDK_ROOT" -- --no-checkout >/dev/null
"$GIT_BIN" -C "$SDK_ROOT" checkout --detach "$SDK_HEAD" >/dev/null

OBSERVED_HEAD="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse HEAD)"
OBSERVED_TREE="$("$GIT_BIN" -C "$SDK_ROOT" rev-parse 'HEAD^{tree}')"
[ "$OBSERVED_HEAD" = "$SDK_HEAD" ] || { echo 'A003: SDK head mismatch.' >&2; exit 19; }
[ "$OBSERVED_TREE" = "$SDK_TREE" ] || { echo 'A003: SDK tree mismatch.' >&2; exit 20; }
[ -z "$("$GIT_BIN" -C "$SDK_ROOT" status --porcelain=v1 -uall)" ] || { echo 'A003: acquired SDK source is dirty.' >&2; exit 21; }

A003_SDK_ROOT="$SDK_ROOT" "$NODE_BIN" -e '
const fs=require("node:fs");
const path=require("node:path");
const root=process.env.A003_SDK_ROOT;
const contract=JSON.parse(fs.readFileSync(path.join(root,"docs/private-continuity/vexrelay/current-contract.json"),"utf8"));
if(contract.contractRef!=="source.vexrelay.current-contract/v1") process.exit(22);
const carrier=contract.acceptedAdapters.find(x=>x.adapterRef==="adapter.vexlocalbridge.native-vexrelay-task-archive/v1");
if(!carrier || carrier.state!=="TERMINAL_ACCEPTED_CURRENT") process.exit(23);
if(!contract.acceptedRecipeFamilies.includes("ExactHeadSourceOwnerProof")) process.exit(24);
'

cat > "$FORMATION_SCRIPT" <<'NODE_A003'
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const SDK_ROOT = process.env.A003_SDK_ROOT;
const OUT = process.env.A003_FORMATION_RESULT;
const TREE = process.env.A003_PRODUCT_TREE;
const DOWNLOADS = path.join(process.env.HOME, 'Downloads');

const { formVexRelayTaskArchiveCarrier } =
  await import(pathToFileURL(path.join(
    SDK_ROOT,
    'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs',
  )).href);

const BASE = 'd857a92371555472e0303b18eadf353da2a868d6';
const HEAD = '04a24d6d08c0afe5d6263db4652eb1e409162fe4';
const BRANCH = 'VXG-112126-component-stable-repeater-proof-a001';
const BASE_BRANCH = 'main';

const CHANGED_PATHS = [
  "architecture/elements/src/test/kotlin/com/vextreme/vexlife/android/elements/VexStableLazyListTest.kt",
];

if (!/^[0-9a-f]{40}$/.test(TREE)) throw new Error('A003_PRODUCT_TREE_INVALID');
if (TREE !== '5e8abdeba1f90c1e5d7287214f909d666658e5e8') throw new Error('A003_PRODUCT_TREE_MISMATCH');

const now = new Date();
const expires = new Date(now.getTime() + 60 * 60 * 1000);

const seed = {
  schemaVersion: 'vex.relay.task/v2',
  continuity: '[VXG RealForever]',
  taskRef: 'task.vexlife-android.pr261.component-stable-repeater.assurance',
  attemptRef: 'attempt.vexlife-android.pr261.component-stable-repeater.assurance.a003.20261122',
  requestRef: 'request.vexlife-android.pr261.component-stable-repeater.assurance.a003.20261122',
  correlationRef: 'correlation.vexlife-android.pr261.component-stable-repeater.assurance.a003.20261122',
  packageRef: 'package.vexlife-android.pr261.component-stable-repeater.assurance.a003.20261122',
  original: {
    originalIntent:
      'Review the exact proved Android Stable Repeater test-only candidate independently from SourceOwner proof without inheriting the authoring lane conclusion.',
    originalGoal:
      'Form fresh exact-head review evidence for Android PR #261 and determine whether the added insert/remove regression strengthens target-native proof without shifting feature, Compose, or portable ownership.',
    userProtectedIntent:
      'Close A4 on truthful target-native evidence while preserving the larger Blueprint rule that completed Product implementations teach reusable relationships without becoming universal semantic authority.',
    sourceRefs: [
      'github.pr.vexlife-android.261',
      'github.pr.vexwork.402',
      'git.vexlife-android.04a24d6d08c0afe5d6263db4652eb1e409162fe4',
      'git.vexlife-android.d857a92371555472e0303b18eadf353da2a868d6',
      'source.vexrelay.current-contract/v1',
      'relay.return.vexlife-android.pr261.source-owner.a002.3812d072c89cb2e36dc8ddd28436d535d3907f751cb29204ccd6e8dd6038eea4',
    ],
  },
  lane: {
    entryClass: 'SAME_LANE_SUCCESSOR',
    scopeMode: 'READ_ONLY',
    laneRef: 'lane.vexlife-android.component-stable-repeater',
    parentRootRef: 'github.pr.vexlife-android.261',
    workRef: 'work.vex.component-library-architecture.target-realizations',
    claimRef: 'claim.vexlife-android.pr261.component-stable-repeater-assurance-readonly',
    returnRouteRef: 'github.pr.vexlife-android.261',
    ownerRef: 'github.pr.vexlife-android.261',
    rootHubRef: 'github.pr.vexwork.402',
  },
  role: {
    activeRole: 'INDEPENDENT_ASSURANCE',
    roleRef: 'role.independent-assurance.vexlife-android.pr261.a003',
    instanceRef: 'instance.vexlife-android.pr261.a003',
    occupancyRef: 'occupancy.vexlife-android.pr261.a003',
    providerBindingRef: 'provider.macos-victor.vexlife-android.pr261.a003',
    threadRef: 'thread.local-vex-operations.vexlife-android.pr261.a003',
    authorityEnvelopeRef: 'authority-envelope.vexlife-android.pr261.assurance.a003',
  },
  capabilities: {
    platformGitHubRead: 'UNAVAILABLE',
    platformGitHubWrite: 'UNAVAILABLE',
  },
  execution: {
    hostProfile: 'macos-victor',
    executionSurface: 'MAC_ZIP_RUN_COMMAND',
    singleUseNonce: 'nonce.vexlife-android.pr261.a003.20261122',
    duplicateAttemptMarker: 'duplicate.vexlife-android.pr261.a003.20261122',
    duplicateProtectionMode: 'OUTER_RELAY_BOUND',
    formedAt: now.toISOString(),
    expiresAtOrNull: expires.toISOString(),
  },
  workspace: {
    mode: 'RUN_ROOT',
    projectRef: 'project.vexlife-android.pr261.component-stable-repeater-assurance',
    projectPath: 'workspace',
    createRoot: true,
    createProjectPath: true,
  },
  repositories: [{
    repositoryRef: 'primary',
    mode: 'CLONE_IN_WORKSPACE',
    cloneTransport: 'GITHUB_GH',
    githubHostname: 'github.com',
    nameWithOwner: 'vgong24/VexLife-Android',
    localPath: 'repositories/primary',
    remote: 'origin',
    baseBranch: BASE_BRANCH,
    acceptedBase: BASE,
    checkoutRef: HEAD,
    expectedHead: HEAD,
    branch: BRANCH,
  }],
  authority: {
    authorityRef: 'authority.vexlife-android.pr261.component-stable-repeater.assurance.a003',
    authorityMode: 'EXACT_STAGE_EFFECTS',
    grantedEffects: [
      'CHECK_AUTH',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    conditionalEffects: [],
    consumedEffects: [],
    remainingEffects: [
      'CHECK_AUTH',
      'QUERY_PROVIDER',
      'FORM_RESULT',
    ],
    heldEffects: [
      'READ_REPOSITORY',
      'RUN_LOCAL_COMMAND',
      'WRITE_SOURCE',
      'PUSH_BRANCH',
      'CREATE_PULL_REQUEST',
      'UPDATE_COORDINATION_METADATA',
      'APPROVE_EXACT_HEAD',
      'MARK_READY',
      'ORDINARY_MERGE',
      'RELEASE_PUBLICATION',
      'PRODUCT_LAUNCH',
      'REAL_HOME_MUTATION',
      'MODEL_RUNTIME_EFFECT',
    ],
    repositoryMembranes: [{
      repositoryRef: 'primary',
      allowedPaths: CHANGED_PATHS,
    }],
    allowedExecutables: [
      '/bin/bash',
    ],
    allowedEnvironment: [
      'HOME',
      'PATH',
      'JAVA_HOME',
      'ANDROID_HOME',
      'ANDROID_SDK_ROOT',
    ],
    remoteObservationOwner: 'Operations',
    allowMutationLoop: false,
    stopConditions: [
      'STOP_ON_UNKNOWN',
      'STOP_ON_PATH_ESCAPE',
      'STOP_ON_AUTH_BLOCK',
      'STOP_ON_EVIDENCE_REJECTED',
    ],
  },
  source: {
    sourceSetRef: 'source-set.vexlife-android.pr261.component-stable-repeater',
    sourceVersionRef: 'git.' + HEAD,
    overlapEvidenceRef: 'github.pr.vexlife-android.261',
    activeConflictState: 'NONE',
    candidateState: 'EXACT',
    candidateHead: HEAD,
    candidateTree: TREE,
    expectedChangedPaths: CHANGED_PATHS.map((p) => ({
      repositoryRef: 'primary',
      path: p,
    })),
    intentionallyUnchangedAuthorizedPaths: [],
  },
  policy: {
    maxOuterReturnFiles: 10,
    maxSelectedSourceEntries: 10,
    maxSelectedSourceBytes: 5000000,
    maxDiffBytes: 10000000,
    requireRacePreflightBeforeMutation: false,
    requireLocalValidationBeforePublish: false,
    requireInputPackageManifest: false,
    signoffCommit: true,
    draftPullRequest: true,
  },
};

const stage = {
  repositoryRef: 'primary',
  expectedBase: BASE,
  expectedHead: HEAD,
  branch: BRANCH,
  maxFiles: 1,
  stateRef: 'state.vexlife-android.pr261.component-stable-repeater.assurance.a003',
  currentLifecycleState: 'ASSURANCE_PENDING',
  currentGoal:
    'Form fresh read-only exact-candidate review evidence for the proved Android Stable Repeater test-only boundary without inheriting SourceOwner semantic clearance.',
  remainingGoals: [
    'Review that #261 changes only proof source and does not change VexStableLazyColumn production behavior.',
    'Review that feature-owned stable refs remain semantic identity and visual/list position remains non-authoritative.',
    'Review that Compose remains target realization and does not become portable semantic owner.',
    'Review that the new insert/remove proof adds pressure only to the already-accepted Stable Repeated Items relationship.',
    'If semantically clear, decide whether A4 may become terminal with only two intentional VexStream Lens-currentness HELDs remaining.',
  ],
  onPass: {
    lifecycleState: 'ASSURANCE_EVIDENCE_READY',
    nextFunction: 'F13.RUN_FRESH_ASSURANCE',
    nextOwner: 'Independent Assurance',
    whyThisIsNext:
      'Exact remote Android candidate evidence is available, but the IndependentAssurance recipe cannot self-issue semantic clearance or A4 completion.',
  },
  resultFilename:
    'RETURN-VEXLIFE-ANDROID-PR261-STABLE-REPEATER-INDEPENDENT-ASSURANCE-A003.zip',
};

const authorityEnvelope = {
  authoritySchemaRef: 'vextreme.effect-transaction/v1',
  semanticAdmissionRef: 'github.pr.vexlife-android.261',
  transactionRef: 'transaction.vexlife-android.pr261.component-stable-repeater.assurance.a003',
  transactionVersion: 1,
  allowedEffectRefs: ['effect.vexrelay.execute'],
  heldEffectRefs: [
    'effect.source.mutation',
    'effect.publication',
    'effect.product.launch',
    'effect.home.mutation',
    'effect.model-runtime',
    'effect.lifecycle',
    'effect.release.publication',
  ],
};

const outerTaskTemplate = {
  schemaVersion: 'vextreme.vexlocalbridge-task-manifest/v1',
  artifactClass: 'EXECUTABLE_RELAY',
  taskRef: 'task.vexlife-android.pr261.component-stable-repeater.carrier',
  attemptRef: 'attempt.vexlife-android.pr261.component-stable-repeater.carrier.a003.20261122',
  packageRef: 'package.vexlife-android.pr261.component-stable-repeater.carrier.a003.20261122',
  requestRef: 'request.vexlife-android.pr261.component-stable-repeater.carrier.a003.20261122',
  correlationRef: 'correlation.vexlife-android.pr261.component-stable-repeater.carrier.a003.20261122',
  taskStorageKey: 'task-vexlife-android-pr261-component-stable-repeater-carrier',
  attemptStorageKey: 'attempt-a003',
  requestingRoleRef: 'IndependentAssurance[VexLife-Android-PR261-A003]',
  requestingInstanceRef: 'instance.independent-assurance.vexlife-android.pr261.a003',
  parentOperationsRef: 'github.pr.vexlife-android.261',
  returnRouteRef: 'github.pr.vexlife-android.261',
  hostProfileRef: 'macos-victor',
  repositoryExecutionProfileRefOrNull:
    'profile.repository.vextreme-sdk.macos-v1',
  repositoryRefOrNull: 'vgong24/VexLife-Android',
  baseRefOrNull: BASE,
  headRefOrNull: HEAD,
  treeRefOrNull: TREE,
  branchRefOrNull: BRANCH,
  claimRefOrNull: 'claim.vexlife-android.pr261.component-stable-repeater-assurance',
  exactPathRefs: CHANGED_PATHS,
  authorityBinding: {
    authoritySchemaRef: 'vextreme.effect-transaction/v1',
    authorityEnvelopePath: 'authority/effect.json',
    authorityEnvelopeSha256: '0'.repeat(64),
    semanticAdmissionRef: 'github.pr.vexlife-android.261',
    transactionRef: 'transaction.vexlife-android.pr261.component-stable-repeater.assurance.a003',
    transactionVersion: 1,
    allowedEffectRefs: ['effect.vexrelay.execute'],
    heldEffectRefs: [
      'effect.source.mutation',
      'effect.publication',
      'effect.product.launch',
      'effect.home.mutation',
      'effect.model-runtime',
      'effect.lifecycle',
      'effect.release.publication',
    ],
  },
  payload: {
    payloadClass: 'VEXRELAY_TASK_ARCHIVE',
    payloadRuntime: 'NATIVE_VEXRELAY',
    payloadPath: 'payload/inner-vexrelay.zip',
    payloadSha256: '0'.repeat(64),
    typedArgv: [],
    immutableInputs: [{
      path: 'inputs/vexrelay-binding.json',
      sha256: '0'.repeat(64),
    }],
  },
  executionStateAtFormation: 'TASK_PREPARED',
  singleUseNonce: 'nonce.vexlife-android.pr261.component-stable-repeater.carrier.a003.20261122',
  stopConditions: [
    'stop.on.identity.drift',
    'stop.on.unadmitted.effect',
  ],
  archiveDigestBinding: 'FILENAME_SHA256',
};

const formed = await formVexRelayTaskArchiveCarrier({
  recipe: 'IndependentAssurance',
  seed,
  stage,
  outerTaskTemplate,
  authorityEnvelope,
  outputDirectory: DOWNLOADS,
  outputStem:
    'VEXLIFE-ANDROID-PR261-STABLE-REPEATER-INDEPENDENT-ASSURANCE-A003',
  ttlMs: 60 * 60 * 1000,
});

fs.writeFileSync(
  OUT,
  JSON.stringify({
    outerZip: formed.outer.finalZip,
    outerSha256: formed.outer.digest,
    innerSha256: formed.inner.sha256,
    taskRef: formed.inner.manifest.taskRef,
    attemptRef: formed.inner.manifest.attemptRef,
    candidateHead: formed.inner.manifest.source.candidateHead,
    candidateTree: formed.inner.manifest.source.candidateTree,
    activeConflictState: formed.inner.manifest.source.activeConflictState,
    qualification: formed.outer.formationEvidence,
  }, null, 2) + '\n',
);
NODE_A003

echo 'A003: forming exact task/carrier...'
A003_SDK_ROOT="$SDK_ROOT" A003_FORMATION_RESULT="$FORMATION_RESULT" A003_PRODUCT_TREE="$PRODUCT_TREE" "$NODE_BIN" "$FORMATION_SCRIPT"

PACKAGE="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerZip)' "$FORMATION_RESULT")"
EXPECTED="$("$NODE_BIN" -e 'const fs=require("node:fs");const x=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(x.outerSha256)' "$FORMATION_RESULT")"

[ -f "$PACKAGE" ] || { echo 'A003: formed package is missing.' >&2; exit 25; }
OBSERVED="$(/usr/bin/shasum -a 256 "$PACKAGE" | /usr/bin/cut -d ' ' -f 1)"
[ "$OBSERVED" = "$EXPECTED" ] || { echo 'A003: formed package digest mismatch.' >&2; exit 26; }

echo "A003: Product head $PRODUCT_HEAD"
echo "A003: Product tree $PRODUCT_TREE"
echo "A003: formed exact package $PACKAGE"
echo "A003: package sha256 $EXPECTED"
echo 'A003: entering current native VexRelay/VexLocalBridge Assurance carrier...'

set +e
VEX_EXPECTED_PACKAGE_SHA256="$EXPECTED" "$BASH_BIN" "$SDK_ROOT/lib/vexlocalbridge/templates/RUN-VEX-TASK.command" "$PACKAGE"
STATUS=$?
set -e

echo "A003: carrier exit $STATUS"
echo 'A003: if a canonical RETURN was formed, upload the exact RETURN ZIP revealed by VexLocalBridge.'
exit "$STATUS"
