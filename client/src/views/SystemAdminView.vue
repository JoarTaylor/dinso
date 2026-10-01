<script setup lang="ts">
import { computed, ref } from 'vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import Panel from '../components/Panel.vue'
import PermissionsDialog from '../components/PermissionsDialog.vue'
import type { ProfilePermissions } from '../components/PermissionsDialog.vue'
import type { Permission } from '../data/customer'

const props = defineProps<{
  title: string
  description: string
  panelTitle: string
  panelDescription: string
  rows: Record<string, string>[]
  companyProfiles: ProfilePermissions[]
  t: (source: string) => string
}>()

const emit = defineEmits<{
  savePermissions: [
    { profileId: string; companyId: string; permissions: Permission[] },
  ]
}>()

const allPermissions: Permission[] = [
  'READ_INFORMATION',
  'APPROVE_CASES',
  'ADD_EMPLOYEES',
  'CHANGE_SALARY',
  'REGISTER_LEAVE_OF_ABSENCE',
  'TERMINATE_EMPLOYMENT',
]

const permissionLabel: Record<Permission, string> = {
  READ_INFORMATION: 'Läs information',
  APPROVE_CASES: 'Godkänn ärenden',
  ADD_EMPLOYEES: 'Lägg till medarbetare',
  CHANGE_SALARY: 'Ändra lön',
  REGISTER_LEAVE_OF_ABSENCE: 'Registrera tjänstledighet',
  TERMINATE_EMPLOYMENT: 'Avsluta anställning',
}

const permissionAbbr: Record<Permission, string> = {
  READ_INFORMATION: 'Läs',
  APPROVE_CASES: 'God',
  ADD_EMPLOYEES: 'Add',
  CHANGE_SALARY: 'Lön',
  REGISTER_LEAVE_OF_ABSENCE: 'Tjl',
  TERMINATE_EMPLOYMENT: 'Avs',
}

// Search & filter state
const search = ref('')
const permissionFilter = ref<Permission | ''>('')

// Dialog state
const editingProfileId = ref<string | null>(null)

const openEditor = (profileId: string): void => {
  editingProfileId.value = profileId
}

const filteredProfiles = computed(() => {
  let result = props.companyProfiles
  const q = search.value.trim().toLowerCase()
  if (q) {
    result = result.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.companies.some((c) => c.companyName.toLowerCase().includes(q)),
    )
  }
  if (permissionFilter.value) {
    const pf = permissionFilter.value
    result = result.filter((p) => p.companies[0]?.permissions.includes(pf))
  }
  return result
})

const editingProfile = computed(
  () => props.companyProfiles.find((p) => p.profileId === editingProfileId.value) ?? null,
)

const primaryPermissions = (profile: ProfilePermissions): Permission[] =>
  profile.companies[0]?.permissions ?? []
</script>

<template>
  <section>
    <PageHeader :title="title" :description="description" />

    <Panel :title="panelTitle" class="permissions-panel">
      <p class="panel-lede">{{ panelDescription }}</p>

      <EmptyState
        v-if="companyProfiles.length === 0"
        :title="t('Inga företagsanvändare')"
        :description="
          t('Det finns inga företagsanvändare i den här kundvarianten.')
        "
      />

      <template v-else>
        <!-- Toolbar -->
        <div class="toolbar">
          <input
            v-model="search"
            class="search-input"
            type="search"
            placeholder="Sök namn eller företag…"
          />
          <select v-model="permissionFilter" class="filter-select">
            <option value="">Alla behörigheter</option>
            <option v-for="perm in allPermissions" :key="perm" :value="perm">
              Har: {{ permissionLabel[perm] }}
            </option>
          </select>
          <span class="result-count">
            {{ filteredProfiles.length }} av {{ companyProfiles.length }}
          </span>
        </div>

        <!-- Table -->
        <div class="table-section">
          <table class="user-table">
            <thead>
              <tr>
                <th class="col-name-h">Namn</th>
                <th class="col-companies-h">Företag</th>
                <th class="col-badges-h">Behörigheter</th>
                <th class="col-action-h"></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="profile in filteredProfiles"
                :key="profile.profileId"
                class="user-row"
                :class="{ 'is-editing': editingProfileId === profile.profileId }"
                @click="openEditor(profile.profileId)"
              >
                <td class="col-name">{{ profile.name }}</td>
                <td class="col-companies">
                  {{ profile.companies.map((c) => c.companyName).join(', ') }}
                </td>
                <td class="col-badges">
                  <span
                    v-for="perm in allPermissions"
                    :key="perm"
                    class="perm-dot"
                    :class="{ active: primaryPermissions(profile).includes(perm) }"
                    :title="permissionLabel[perm]"
                  >
                    {{ permissionAbbr[perm] }}
                  </span>
                </td>
                <td class="col-action">
                  <button class="edit-btn" @click.stop="openEditor(profile.profileId)">
                    Redigera
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <p v-if="filteredProfiles.length === 0" class="no-results">
            Inga användare matchar sökningen.
          </p>
        </div>

        <!-- Permissions dialog -->
        <PermissionsDialog
          v-if="editingProfile"
          :profile="editingProfile"
          @close="editingProfileId = null"
          @save-permissions="emit('savePermissions', $event)"
        />
      </template>
    </Panel>
  </section>
</template>

<style scoped>
.panel-lede {
  max-width: 64ch;
  margin: 0 0 20px;
  color: var(--muted);
  line-height: 1.55;
}

.permissions-panel {
  margin-top: 24px;
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  font-size: 0.875rem;
  background: var(--surface);
  color: var(--ink);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent, #0057b7);
  box-shadow: 0 0 0 2px
    color-mix(in srgb, var(--accent, #0057b7) 20%, transparent);
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  font-size: 0.875rem;
  background: var(--surface);
  color: var(--ink);
  cursor: pointer;
}

.result-count {
  font-size: 0.8rem;
  color: var(--muted);
  white-space: nowrap;
}

.table-section {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

/* Table */
.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.user-table thead th {
  text-align: left;
  padding: 8px 10px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.user-table tbody tr {
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.1s;
}

.user-table tbody tr:last-child {
  border-bottom: none;
}

.user-table tbody tr:hover {
  background: var(--surface-hover, var(--border));
}

.user-table tbody tr.is-editing {
  background: color-mix(in srgb, var(--accent, #0057b7) 6%, transparent);
}

.user-table td {
  padding: 9px 10px;
  vertical-align: middle;
}

.col-name {
  font-weight: 600;
  white-space: nowrap;
}

.col-companies {
  color: var(--muted);
  font-size: 0.82rem;
}

.col-badges {
  white-space: nowrap;
}

/* Permission badges */
.perm-dot {
  display: inline-block;
  padding: 2px 5px;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  margin-right: 3px;
  background: var(--border);
  color: var(--muted);
  transition:
    background 0.1s,
    color 0.1s;
}

.perm-dot.active {
  background: color-mix(in srgb, var(--accent, #0057b7) 15%, transparent);
  color: var(--accent, #0057b7);
}

.col-action {
  text-align: right;
  white-space: nowrap;
}

.edit-btn {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control, 6px);
  background: var(--surface);
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink);
}

.edit-btn:hover {
  background: var(--surface-hover, var(--border));
}

.no-results {
  padding: 24px 0;
  text-align: center;
  color: var(--muted);
  font-size: 0.875rem;
}


/* ── Mobile ── */
@media (max-width: 640px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    min-width: 0;
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }

  .result-count {
    text-align: right;
  }

  /* Hide companies and badges columns — too cramped */
  .col-companies-h,
  .col-companies,
  .col-badges-h,
  .col-badges {
    display: none;
  }

  .col-name {
    font-size: 0.85rem;
  }

  .edit-btn {
    padding: 6px 12px;
    font-size: 0.85rem;
  }

}
</style>
