<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-vue-next';

const props = defineProps<{
  page: number;
  pageSize: number;
  total: number;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  change: [page: number];
}>();

function go(delta: number): void {
  const next = props.page + delta;
  goTo(next);
}

function goTo(next: number): void {
  const target = Math.max(1, Math.min(next, totalPages.value));
  if (target !== props.page && !props.disabled) {
    emit('change', target);
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
const visiblePages = computed(() => {
  const pages: number[] = [];
  const start = Math.max(1, Math.min(props.page - 2, totalPages.value - 4));
  const end = Math.min(totalPages.value, start + 4);
  for (let current = start; current <= end; current++) {
    pages.push(current);
  }
  return pages;
});
</script>

<template>
  <div class="pagination-bar">
    <span>第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
    <div class="pager-actions">
      <button type="button" title="第一页" :disabled="disabled || page <= 1" @click="goTo(1)">
        <ChevronsLeft :size="16" />
      </button>
      <button type="button" title="上一页" :disabled="disabled || page <= 1" @click="go(-1)">
        <ChevronLeft :size="16" />
      </button>
      <button
        v-for="pageNumber in visiblePages"
        :key="pageNumber"
        type="button"
        class="page-number"
        :title="`第 ${pageNumber} 页`"
        :aria-label="`第 ${pageNumber} 页`"
        :class="{ active: pageNumber === page }"
        :aria-current="pageNumber === page ? 'page' : undefined"
        :disabled="disabled || pageNumber === page"
        @click="goTo(pageNumber)"
      >
        {{ pageNumber }}
      </button>
      <button type="button" title="下一页" :disabled="disabled || page >= totalPages" @click="go(1)">
        <ChevronRight :size="16" />
      </button>
      <button type="button" title="最后一页" :disabled="disabled || page >= totalPages" @click="goTo(totalPages)">
        <ChevronsRight :size="16" />
      </button>
    </div>
  </div>
</template>
